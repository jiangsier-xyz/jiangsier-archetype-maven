#!/usr/bin/env python3
"""
sync-back.py — write edits made in a generated project back into the archetype.

Run after editing a generated project (e.g. one produced by bin/gen-proj.sh)
to verify a fix in real Java/Maven, then push those edits back into
src/main/resources/archetype-resources/ for the next generation.

Strategy: per file, forward-substitute the existing archetype source to
predict what the archetype plugin would have produced, line-diff it against
the user's edited generated file, and patch only the changed hunks back into
the archetype source — applying reverse substitutions to the newly-introduced
lines. Unchanged hunks come straight from the existing archetype source, so
existing Velocity escape conventions (${symbol_dollar}, ${symbol_pound},
leading #set directives) are preserved automatically.

Files inside `configs/`, `scripts/`, and `__rootArtifactId__-start/src/main/resources/static/`
are treated as unfiltered (no substitution; verbatim copy) per the archetype's
metadata.

Usage: bin/sync-back.py [--from DIR] [--group-id ID] [--artifact-id ID]
                       [--archetype-resources DIR] [--only GLOB] [--dry-run] [-v]

Exit codes: 0 on success, 1 on bad usage, 2 if any file failed to write.
"""

from __future__ import annotations

import argparse
import difflib
import re
import sys
from dataclasses import dataclass
from pathlib import Path

MODULE_SUFFIXES = ("common", "dal", "service", "start")

EXCLUDE_DIR_NAMES = frozenset({
    "target", ".git", ".idea", ".gradle", "node_modules",
    "logs", "local-data",
})
EXCLUDE_FILE_NAMES = frozenset({".DS_Store"})

# Subtrees that the archetype copies verbatim (no Velocity filtering).
# Paths are relative to archetype-resources/ (i.e. they use __rootArtifactId__).
UNFILTERED_PREFIXES = (
    "configs/",
    "scripts/",
    "__rootArtifactId__-start/src/main/resources/static/",
)

# Match a leading Velocity #set( $foo = ... ) directive.
SET_LINE_RE = re.compile(r"^\s*#set\s*\(\s*\$\w+\s*=.*?\)\s*$")


@dataclass(frozen=True)
class Coords:
    group_id: str
    artifact_id: str

    @property
    def package(self) -> str:
        return self.group_id

    @property
    def package_dir(self) -> str:
        return self.group_id.replace(".", "/")

    @property
    def package_depth(self) -> int:
        return len(self.group_id.split("."))

    @property
    def app_domain(self) -> str:
        return ".".join(reversed(self.group_id.split(".")))

    @property
    def github_namespace(self) -> str:
        return "-".join(reversed(self.group_id.split(".")))

    @property
    def image_repository(self) -> str:
        return f"{self.group_id}/{self.artifact_id}"

    @property
    def scm_connection(self) -> str:
        return f"scm:git:https://github.com/{self.github_namespace}/{self.artifact_id}.git"

    @property
    def scm_url(self) -> str:
        return f"https://github.com/{self.github_namespace}/{self.artifact_id}"


# ---- Velocity forward / reverse substitution ------------------------------


def forward_subs(c: Coords) -> list[tuple[str, str]]:
    """${...} placeholders -> their concrete values, in the order Velocity
    would resolve them. The archetype's #set( $symbol_dollar = '$' ) etc. are
    handled here too so a templated ${symbol_dollar}{X} renders as ${X}."""
    return [
        ("${symbol_dollar}", "$"),
        ("${symbol_pound}", "#"),
        ("${symbol_escape}", "\\"),
        ("${scmConnection}", c.scm_connection),
        ("${scmUrl}", c.scm_url),
        ("${imageRepository}", c.image_repository),
        ("${appDomain}", c.app_domain),
        *[(f"${{rootArtifactId}}-{m}", f"{c.artifact_id}-{m}") for m in MODULE_SUFFIXES],
        ("${rootArtifactId}", c.artifact_id),
        ("${packageInPathFormat}", c.package_dir),
        ("${packageDir}", c.package_dir),
        ("${package}", c.package),
        ("${groupId}", c.group_id),
        # ${artifactId} is module-dependent; resolved per-file by caller.
        ("${revision}", "0.0.1-SNAPSHOT"),
        ("${version}", "0.0.1-SNAPSHOT"),
    ]


def reverse_subs(c: Coords) -> list[tuple[str, str]]:
    """Concrete values -> ${...} placeholders. Order matters: longer / more
    specific patterns first so they don't get partially eaten by simpler ones."""
    subs: list[tuple[str, str]] = []
    subs.append((c.scm_connection, "${scmConnection}"))
    subs.append((c.scm_url, "${scmUrl}"))
    subs.append((c.image_repository, "${imageRepository}"))
    subs.append((c.app_domain, "${appDomain}"))
    for m in MODULE_SUFFIXES:
        subs.append((f"{c.artifact_id}-{m}", "${rootArtifactId}-" + m))
    subs.append((c.artifact_id, "${rootArtifactId}"))
    subs.append((c.package_dir, "${packageDir}"))
    subs.append((c.package, "${package}"))
    return subs


def apply_pairs(content: str, pairs: list[tuple[str, str]]) -> str:
    for a, b in pairs:
        content = content.replace(a, b)
    return content


# ---- Preamble (leading #set directives) -----------------------------------


def split_preamble(content: str) -> tuple[str, str]:
    """Return (preamble, body). Preamble is the leading run of #set( ... ) lines.
    Body is everything else, untouched."""
    lines = content.splitlines(keepends=True)
    end = 0
    for line in lines:
        if SET_LINE_RE.match(line):
            end += 1
        else:
            break
    return "".join(lines[:end]), "".join(lines[end:])


# ---- Path mapping ----------------------------------------------------------


def map_path(rel: Path, c: Coords) -> Path | None:
    """Generated-project-relative path -> archetype-resources-relative path."""
    parts = rel.parts
    if not parts:
        return None

    if len(parts) == 1:
        return Path(parts[0])

    first = parts[0]

    if first in ("configs", "scripts"):
        return Path(*parts)

    for m in MODULE_SUFFIXES:
        if first == f"{c.artifact_id}-{m}":
            new_first = f"__rootArtifactId__-{m}"
            inner = strip_packaged_prefix(list(parts[1:]), c)
            return Path(new_first, *inner)

    return None


def strip_packaged_prefix(parts: list[str], c: Coords) -> list[str]:
    """For paths under src/{main,test}/java/, drop the <package-dir> prefix
    that maven-archetype prepends for `packaged="true"` filesets."""
    if len(parts) < 3 + c.package_depth:
        return parts
    if parts[0] != "src" or parts[1] not in ("main", "test") or parts[2] != "java":
        return parts
    pkg_parts = c.package.split(".")
    if parts[3 : 3 + c.package_depth] != pkg_parts:
        return parts
    return parts[:3] + parts[3 + c.package_depth :]


def is_unfiltered(target_rel: Path) -> bool:
    s = target_rel.as_posix() + ("/" if target_rel.is_dir() else "")
    return any(s.startswith(p) or s == p.rstrip("/") for p in UNFILTERED_PREFIXES) or \
        any(s.startswith(p) for p in UNFILTERED_PREFIXES)


# ---- Module-aware ${artifactId} resolution --------------------------------


def file_artifact_id(target_rel: Path, c: Coords) -> str:
    """Resolve ${artifactId} for this file. Maven-archetype scopes ${artifactId}
    per fileSet: for top-level filesets it's the root artifact id; for module
    filesets it's the module's own artifact id."""
    parts = target_rel.parts
    if parts and parts[0].startswith("__rootArtifactId__-"):
        return f"{c.artifact_id}-{parts[0][len('__rootArtifactId__-'):]}"
    return c.artifact_id


# Files we never sync. pom.xml is in here because maven-archetype rewrites
# poms during generation (extracting parent refs, merging dependencyManagement)
# so they don't round-trip through textual diff.
SKIP_PATHS = frozenset({"pom.xml"})


def should_skip(target_rel: Path) -> bool:
    name = target_rel.name
    if name in SKIP_PATHS:
        return True
    return False


# ---- File walking ----------------------------------------------------------


def iter_generated_files(root: Path) -> list[Path]:
    """Walk root, skipping known build/IDE subtrees. Exclusions are checked
    relative to root so a generated project that itself lives under target/
    isn't wholly skipped."""
    out: list[Path] = []
    for path in root.rglob("*"):
        if not path.is_file():
            continue
        if path.name in EXCLUDE_FILE_NAMES:
            continue
        rel_parts = path.relative_to(root).parts
        if any(part in EXCLUDE_DIR_NAMES for part in rel_parts[:-1]):
            continue
        out.append(path)
    return out


def is_text_file(path: Path) -> bool:
    try:
        with path.open("rb") as f:
            chunk = f.read(8192)
    except OSError:
        return False
    return b"\x00" not in chunk


# ---- Core transform --------------------------------------------------------


def transform_filtered(
    actual_generated: str,
    existing_archetype: str | None,
    forward: list[tuple[str, str]],
    reverse: list[tuple[str, str]],
    extra_forward: list[tuple[str, str]] = (),
) -> str:
    """Patch the existing archetype source so it would re-generate to
    `actual_generated`. Lines that don't change (vs the predicted generation)
    are taken from the existing archetype source, preserving its escape style.
    Changed/added lines come from `actual_generated` with reverse substitution."""
    if existing_archetype is None:
        # New file. Best-effort: just apply reverse substitution to the actual
        # generated content. The user must add any needed #set preamble manually.
        return apply_pairs(actual_generated, reverse)

    preamble, body = split_preamble(existing_archetype)
    # Predict what the archetype plugin would produce from this body.
    predicted = apply_pairs(body, list(extra_forward) + forward)
    if predicted == actual_generated:
        return existing_archetype

    body_lines = body.splitlines(keepends=True)
    predicted_lines = predicted.splitlines(keepends=True)
    actual_lines = actual_generated.splitlines(keepends=True)

    # body_lines and predicted_lines are line-aligned: forward subst is purely
    # textual replacement that doesn't add or remove newlines, only changes
    # within lines. So opcodes computed against predicted apply to body too.
    if len(body_lines) != len(predicted_lines):
        # Defensive: fall back to whole-file reverse substitution rather than
        # produce a corrupt output.
        return preamble + apply_pairs(actual_generated, reverse)

    matcher = difflib.SequenceMatcher(a=predicted_lines, b=actual_lines, autojunk=False)
    out: list[str] = []
    for tag, i1, i2, j1, j2 in matcher.get_opcodes():
        if tag == "equal":
            out.extend(body_lines[i1:i2])
        else:  # replace, insert, delete
            new_lines = actual_lines[j1:j2]
            for line in new_lines:
                out.append(apply_pairs(line, reverse))
    return preamble + "".join(out)


# ---- Main ------------------------------------------------------------------


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Sync edits from a generated project back into archetype-resources.",
    )
    repo_root = Path(__file__).resolve().parent.parent
    parser.add_argument(
        "--from", dest="from_dir", default=str(repo_root / "target/test-gen-proj"),
        help="generated project's parent dir or the project dir itself "
             "(default: <repo>/target/test-gen-proj)",
    )
    parser.add_argument("--group-id", default="xyz.jiangsier")
    parser.add_argument("--artifact-id", default="jiangsier-archetype-demo")
    parser.add_argument(
        "--archetype-resources",
        default=str(repo_root / "src/main/resources/archetype-resources"),
    )
    parser.add_argument("--only", default=None,
                        help="glob (relative to generated project root) limiting which files are synced")
    parser.add_argument("--dry-run", action="store_true")
    parser.add_argument("-v", "--verbose", action="store_true")
    args = parser.parse_args()

    coords = Coords(args.group_id, args.artifact_id)
    src_root = Path(args.from_dir)
    if (src_root / args.artifact_id).is_dir():
        src_root = src_root / args.artifact_id
    if not src_root.is_dir():
        print(f"error: generated project not found at {src_root}", file=sys.stderr)
        return 1

    dst_root = Path(args.archetype_resources)
    if not dst_root.is_dir():
        print(f"error: archetype-resources not found at {dst_root}", file=sys.stderr)
        return 1

    forward = forward_subs(coords)
    reverse = reverse_subs(coords)

    written = 0
    unchanged = 0
    unmapped = 0
    failed = 0

    for src_file in iter_generated_files(src_root):
        rel = src_file.relative_to(src_root)
        if args.only and not rel.match(args.only):
            continue

        target_rel = map_path(rel, coords)
        if target_rel is None:
            if args.verbose:
                print(f"skip (unmapped): {rel}", file=sys.stderr)
            unmapped += 1
            continue

        if should_skip(target_rel):
            if args.verbose:
                print(f"skip (always-skip): {target_rel}", file=sys.stderr)
            unmapped += 1
            continue

        dst_file = dst_root / target_rel
        unfiltered = is_unfiltered(target_rel)

        try:
            src_bytes = src_file.read_bytes()
        except OSError as e:
            print(f"error reading {src_file}: {e}", file=sys.stderr)
            failed += 1
            continue

        existing_bytes: bytes | None = None
        if dst_file.exists():
            try:
                existing_bytes = dst_file.read_bytes()
            except OSError as e:
                print(f"error reading {dst_file}: {e}", file=sys.stderr)
                failed += 1
                continue

        # Binary or unfiltered files: byte-for-byte copy.
        if unfiltered or not is_text_file(src_file):
            if existing_bytes == src_bytes:
                unchanged += 1
                continue
            if args.dry_run:
                print(f"would write: {target_rel}")
            else:
                dst_file.parent.mkdir(parents=True, exist_ok=True)
                dst_file.write_bytes(src_bytes)
                if args.verbose:
                    print(f"wrote: {target_rel}")
            written += 1
            continue

        try:
            actual = src_bytes.decode("utf-8")
            existing = existing_bytes.decode("utf-8") if existing_bytes is not None else None
        except UnicodeDecodeError as e:
            print(f"error decoding {src_file}: {e}", file=sys.stderr)
            failed += 1
            continue

        # ${artifactId} resolves to the root for top-level filesets and to the
        # module's own artifact id inside __rootArtifactId__-MODULE/.
        extra_forward: list[tuple[str, str]] = [
            ("${artifactId}", file_artifact_id(target_rel, coords)),
        ]

        new_content = transform_filtered(actual, existing, forward, reverse, extra_forward)
        if existing == new_content:
            unchanged += 1
            continue

        if args.dry_run:
            print(f"would write: {target_rel}")
        else:
            dst_file.parent.mkdir(parents=True, exist_ok=True)
            dst_file.write_text(new_content, encoding="utf-8")
            if args.verbose:
                print(f"wrote: {target_rel}")
        written += 1

    summary = (
        f"sync-back: {written} written, {unchanged} unchanged, "
        f"{unmapped} unmapped, {failed} failed"
        + (" (dry-run)" if args.dry_run else "")
    )
    print(summary, file=sys.stderr)
    return 2 if failed else 0


if __name__ == "__main__":
    sys.exit(main())
