# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this repo is

This is a **Maven archetype** (`<packaging>maven-archetype</packaging>`), not a runnable application. It generates a multi-module Spring Boot 4 / Java 25 web-app skeleton (MySQL + Redis, MyBatis, Redisson, Spring Security with portal/OAuth2/API-token auth, Helm charts, Docker build).

Two layers exist in this repo and they are easy to confuse:

- **The archetype itself** — top-level `pom.xml`, `bin/gen-proj.sh`, `archetype-catalog.xml`, and `src/main/resources/META-INF/maven/archetype-metadata.xml`. Edits here change how projects are generated.
- **The archetype payload** — everything under `src/main/resources/archetype-resources/`. These are **template files** that get filtered (Velocity-substituted) and copied into a new project at generation time. References like `${rootArtifactId}`, `${groupId}`, `${package}`, `__rootArtifactId__` (in directory names) are placeholders the archetype plugin substitutes — they are not bugs and must be preserved when editing.

When the README discusses "awesome-app" features (Distributed Cache, Spring Session, Authentication, `@Trace`, etc.), it is describing the **generated** application — the source for those features lives under `archetype-resources/__rootArtifactId__-{common,dal,service,start}/`.

## Common commands

### Building / installing the archetype

```bash
mvn clean install                         # build + install archetype to local ~/.m2
mvn -Pspotless spotless:check             # lint POM (only file enforced at this layer)
mvn -Pspotless spotless:apply             # auto-fix
```

The top-level project has only one source file (`pom.xml`) plus templates — there are no Java tests at this layer. Don't expect `mvn test` to run anything meaningful here.

### Generating a project from the archetype (smoke test your changes)

```bash
bin/gen-proj.sh --group-id xyz.jiangsier \
  --artifact-id jiangsier-archetype-demo \
  --image-repository jiangsier/jiangsier-archetype-demo \
  -o /tmp/archetype-out
```

`gen-proj.sh` runs `mvn clean install` on this archetype, then `mvn archetype:generate` against it. To verify a change to template content, regenerate and inspect/build the output project. See `bin/gen-proj.sh:147` for the full property list passed to `archetype:generate` (e.g. `appDomain`, `scmConnection`, `imageRepository`).

### Working inside a generated project

Generated projects ship a `makefile` (see `archetype-resources/makefile`):

```bash
make build            # mvn -U clean test
make lint             # mvn -T12C -Pspotless spotless:check  (ratchets from origin/main)
make format           # mvn -T12C -Pspotless spotless:apply
make lint-all         # check every file regardless of git diff
make format-all       # format every file regardless of git diff
```

Run a single test in the generated project: `mvn -pl <module> test -Dtest=ClassName#method`. Integration tests (`*IT`) are run by `failsafe` (`mvn verify`).

The generated project's `scripts/` directory contains operational scripts — `build.sh` (mvn package + docker buildx), `local-deps.sh` / `local-run.sh` (Docker MySQL+Redis for local dev), `mbg.sh` (regenerate MyBatis DAL from `schema.sql` via a throwaway MySQL container), `install.sh` / `upgrade.sh` / `uninstall.sh` (Helm), `port-forward.sh` (k8s remote-debug helper). All of them source `setenv.sh`, which parses Helm `values.yaml` into `HELM_*` env vars.

## Spotless / code style

- Java is formatted by **palantir-java-format** (configured in `archetype-resources/pom.xml`).
- POM files are sorted with `sortPom` (4-space indent).
- The default `spotless` profile uses `<ratchetFrom>origin/main</ratchetFrom>` — only files changed vs. `origin/main` are checked. Use the `spotless-all` profile to enforce on everything.
- The top-level archetype `pom.xml` also has a spotless config that ratchets from `origin/main` for the POM only.

## Architecture of the generated application

Four Maven modules under `archetype-resources/`, each prefixed `__rootArtifactId__-` (substituted to the user's artifactId on generation):

- **`-common`** — Shared utilities. Notably `util/SpELUtils.java`, used by the `@Trace`, `@Secret`, `@ShortPeriodCache` / `@MiddlePeriodCache` / `@LongPeriodCache` annotations to evaluate SpEL expressions over method arguments.
- **`-dal`** — MyBatis data-access layer. `src/main/resources/sql/schema.sql` is the source of truth for the schema; `mybatis-generator/generatorConfig.xml` drives MGB. After editing the schema, regenerate the DAL with `scripts/mbg.sh` rather than hand-editing generated mappers.
- **`-service`** — Business services. Cache key generation lives here (`cache/FullNameKeyGenerator.java`).
- **`-start`** — Spring Boot application + web layer. Contains `Application.java`, `controller/`, `config/` (Redisson cache + session config), `access/auth/` (Spring Security wiring, OAuth2 customizers, API-token auth), `access/trace/` (`@Trace` aspect, HTTP trace interceptor). `application-online.yml` is rendered from Helm `_spring.tpl` at install time and mounted as a Secret named `<app>-spring-properties`.

### Cross-cutting concerns to be aware of when editing

- **Distributed cache & session** are Redisson-backed Spring Cache and Spring Session. The Redisson↔spring-session-data-redis version pairing is strict — see `archetype-resources/pom.xml` (`redisson-spring-data-40` for Spring Data Redis 4.0.x). Bumping `spring-boot.version` may force a matching `redisson-spring-data-XX` swap.
- **OAuth2** uses a per-provider `OAuth2AuthorizationRequestCustomizer` lookup-by-bean-name pattern (Google, Aliyun supported out of the box). Adding a new provider means adding another customizer bean named to match the registrationId. A `bind=1` query parameter on the authorization URL switches OAuth2 from "login" to "account binding" semantics — keep that branch in mind when touching auth flows.
- **API token auth** reads tokens from (in priority order) a query param (default `_token`), a header (default `X-API-TOKEN`), or `Authorization: Bearer`. Tokens are full-scope only despite the schema supporting scopes.
- **`@Trace`** writes structured perf logs to `logs/common-perf.log`; status field is `S/F/B` (success/failure/bad-request — `B` doesn't count toward failure rate). `@Secret` on a parameter masks its value in the log.
- **Helm is the single source of config truth.** The Spring `application-online.yml` is rendered from `configs/helm/templates/backend/_spring.tpl` so MySQL URL, Redis URL, secrets, etc. live in `values.yaml` rather than being duplicated. When debugging a missing/wrong runtime property, trace it back through `_spring.tpl` and `deployment.yaml` first.
- **Compile flag `-parameters`** is set, so SpEL in `@Trace`/`@Cache*` annotations can reference parameters by name (`#username`) instead of `#p1`.

## Editing template files — gotchas

- Files under `archetype-resources/` are filtered with Velocity. A literal `$` followed by `{` in Java/YAML/SQL must be escaped (e.g. `${esc.d}{...}` or `\${...}`) or it will be eaten at generation time.
- Directory names like `__rootArtifactId__-start` are renamed by the archetype plugin — don't rename them to a literal artifact id.
- `archetype-metadata.xml` declares which directories are filtered vs copied verbatim and which are `packaged` (placed under the user's `package` path). Adding a new module or top-level directory requires a corresponding entry there.
- `archetype-resources/README.md` and `archetype-resources/README.zh-CN.md` are templated copies of the top-level READMEs — keep them in sync if you change user-facing docs.
