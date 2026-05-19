#!/usr/bin/env bash
#
# test-gen-proj.sh — smoke-test the archetype: generate a project with
# bin/gen-proj.sh, run any preprocessing hooks, and verify the result builds
# with Maven.
#
# Designed to run both locally and in CI (see
# .github/workflows/test-archetype.yml). Returns non-zero on any failure;
# preserves the generated project on failure for inspection.
#
# Exit codes:
#   0  success
#   1  bad usage / configuration
#   2  project generation failed
#   3  hook failed
#   4  maven build failed

set -euo pipefail

repo_path=$(cd "$(dirname "${BASH_SOURCE[0]}")/.."; pwd)

# --- defaults ----------------------------------------------------------------
group_id="xyz.jiangsier"
artifact_id="jiangsier-archetype-demo"
# target/ is already in .gitignore at the repo root; reusing it keeps the
# workspace clean without extra ignore rules.
output_dir="${repo_path}/target/test-gen-proj"
hooks_dir="${repo_path}/bin/test-hooks"
goal="clean compile"
cleanup="on-success"   # always | never | on-success
verbose=0

# --- helpers -----------------------------------------------------------------
log() {
  printf '[test-gen-proj] %s %s\n' "$(date '+%H:%M:%S')" "$*" >&2
}

die() {
  log "ERROR: $1"
  exit "${2:-1}"
}

usage() {
  cat <<EOF
Usage: bin/test-gen-proj.sh [options]

Generates a project from this archetype, runs any preprocessing hooks, and
verifies the result builds with Maven.

Options:
      --group-id <id>     groupId for the generated project (default: ${group_id})
      --artifact-id <id>  artifactId for the generated project (default: ${artifact_id})
      --output <dir>      where the generated project is written
                          (default: <repo>/target/test-gen-proj)
      --hooks-dir <dir>   directory of executable *.sh hooks run in lexical
                          order before the Maven build, each with CWD set to
                          the generated project root
                          (default: <repo>/bin/test-hooks)
      --goal "<args>"     Maven goals/phases passed to mvn — word-split, so
                          'clean test -DskipITs' works (default: "${goal}")
      --cleanup <mode>    always | never | on-success (default: ${cleanup})
  -v, --verbose           shell tracing + mvn -X
  -h, --help              this help

Hook scripts inherit these env vars:
  ARCHETYPE_REPO          repo containing this archetype
  GENERATED_PROJECT_DIR   absolute path to the freshly generated project
  GROUP_ID, ARTIFACT_ID   coordinates the project was generated with
EOF
}

# --- arg parsing -------------------------------------------------------------
while [ $# -gt 0 ]; do
  case "$1" in
    --group-id)    group_id="$2"; shift 2 ;;
    --artifact-id) artifact_id="$2"; shift 2 ;;
    --output)      output_dir="$2"; shift 2 ;;
    --hooks-dir)   hooks_dir="$2"; shift 2 ;;
    --goal)        goal="$2"; shift 2 ;;
    --cleanup)     cleanup="$2"; shift 2 ;;
    -v|--verbose)  verbose=1; shift ;;
    -h|--help)     usage; exit 0 ;;
    *)             usage >&2; die "unknown argument: $1" ;;
  esac
done

case "${cleanup}" in
  always|never|on-success) ;;
  *) die "--cleanup must be one of: always, never, on-success (got: ${cleanup})" ;;
esac

[[ "${verbose}" -eq 1 ]] && set -x

generated_project_dir="${output_dir}/${artifact_id}"

# --- step 1: clean output directory -----------------------------------------
log "preparing output directory: ${output_dir}"
rm -rf "${output_dir}"
mkdir -p "${output_dir}"

# --- step 2: generate the project -------------------------------------------
log "generating ${group_id}:${artifact_id}"
gen_args=(
  --group-id "${group_id}"
  --artifact-id "${artifact_id}"
  --output "${output_dir}"
)
[[ "${verbose}" -eq 1 ]] && gen_args+=(--verbose)

"${repo_path}/bin/gen-proj.sh" "${gen_args[@]}" \
  || die "gen-proj.sh failed" 2

# gen-proj.sh's own destructive `rm -rf <artifact-id>` runs inside output_dir,
# but if the archetype plugin silently no-ops we want to fail loudly instead
# of running mvn against a bogus path.
[[ -d "${generated_project_dir}" ]] \
  || die "expected generated project at ${generated_project_dir} but it was not created" 2

# --- step 3: run preprocessing hooks ----------------------------------------
if [[ -d "${hooks_dir}" ]]; then
  # nullglob so no-match expands to empty; bash glob is already lexically
  # sorted, which gives a stable, predictable hook order (10-foo.sh, 20-bar.sh).
  shopt -s nullglob
  hook_candidates=( "${hooks_dir}"/*.sh )
  shopt -u nullglob

  hooks=()
  for f in "${hook_candidates[@]}"; do
    [[ -x "$f" ]] && hooks+=("$f")
  done

  if [[ ${#hooks[@]} -gt 0 ]]; then
    log "running ${#hooks[@]} hook(s) from ${hooks_dir}"
    export ARCHETYPE_REPO="${repo_path}"
    export GENERATED_PROJECT_DIR="${generated_project_dir}"
    export GROUP_ID="${group_id}"
    export ARTIFACT_ID="${artifact_id}"
    for hook in "${hooks[@]}"; do
      log "  hook: $(basename "${hook}")"
      # Subshell isolates `cd` and any env changes the hook makes from
      # the rest of the script.
      ( cd "${generated_project_dir}" && "${hook}" ) \
        || die "hook failed: ${hook}" 3
    done
  else
    log "no executable *.sh hooks in ${hooks_dir}, skipping"
  fi
else
  log "no hooks directory at ${hooks_dir}, skipping"
fi

# --- step 4: maven build -----------------------------------------------------
log "running mvn ${goal}"
mvn_args=(-B -ntp)
[[ "${verbose}" -eq 1 ]] && mvn_args+=(-X)
# ${goal} is intentionally unquoted: callers pass multi-word values like
# "clean test -DskipITs" via --goal and rely on word-splitting here.
# shellcheck disable=SC2086
( cd "${generated_project_dir}" && mvn "${mvn_args[@]}" ${goal} ) \
  || die "maven build failed (output preserved at ${generated_project_dir})" 4

# --- step 5: cleanup ---------------------------------------------------------
case "${cleanup}" in
  always|on-success)
    log "removing ${output_dir}"
    rm -rf "${output_dir}"
    ;;
  never)
    log "keeping ${output_dir}"
    ;;
esac

log "OK"
