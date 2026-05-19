#!/usr/bin/env bash
#
# gen-proj.sh — install this archetype into the local Maven repo and generate a
# new project from it.
#
# Required: --group-id, --artifact-id
# Optional: --version, --package, --image-repository, --scm-connection,
#           --scm-url, -o|--output, -v|--verbose
#
# Defaults derived from --group-id / --artifact-id (see "derive defaults" block):
#   package          = <group-id>
#   image-repository = <group-id>/<artifact-id>
#   scm-connection   = scm:git:https://github.com/<reverse(group-id)>/<artifact-id>.git
#   scm-url          = https://github.com/<reverse(group-id)>/<artifact-id>
#   appDomain        = reverse(group-id)
#
# Side effects:
#   - writes to ~/.m2/ and ~/.m2/repository/
#   - runs `mvn clean install` on this archetype project
#   - if a directory matching --artifact-id exists in the output dir, it is
#     deleted before generation (destructive — see the rm -rf below)

project_path=$(cd $(dirname ${BASH_SOURCE[0]})/..; pwd)
archetype_group_id=xyz.jiangsier
archetype_artifact_id=jiangsier-archetype-maven
archetype_version=1.0.0
group_id=
artifact_id=
version=1.0.0-SNAPSHOT
package=
package_dir=
image_repository=
scm_connection=
scm_url=
domain=
output=
home=${HOME:-~}
m2home=${home}/.m2

# Reverse a dotted identifier: "xyz.jiangsier" -> "jiangsier.xyz".
# Used to turn a Java-style groupId into a reverse-DNS app domain.
function to_domain {
  arr=($(echo ${1//./ }))
  domain_by_group_id=$(printf '%s\n' "${arr[@]}" | tac | tr '\n' '.')
  echo ${domain_by_group_id%.}
}

# Same as to_domain but joined with '-' instead of '.', for use as a default
# GitHub org name in the SCM URL (xyz.jiangsier -> jiangsier-xyz).
function to_namespace {
  arr=($(echo ${1//./ }))
  ns_by_group_id=$(printf '%s\n' "${arr[@]}" | tac | tr '\n' '-')
  echo ${ns_by_group_id%-}
}

while [ $# -gt 0 ]
do
  case $1 in
    --group-id)
      group_id=$2
      shift
      shift
      ;;
    --artifact-id)
      artifact_id=$2
      shift
      shift
      ;;
    --version)
      version=$2
      shift
      shift
      ;;
    --package)
      package=$2
      shift
      shift
      ;;
    --image-repository)
      image_repository=$2
      shift
      shift
      ;;
    --scm-connection)
      scm_connection=$2
      shift
      shift
      ;;
    --scm-url)
      scm_url=$2
      shift
      shift
      ;;
    -o|--output)
      output=$2
      shift
      shift
      ;;
    -v|--verbose)
      # -eux: trace every command and abort on first error.
      # -X: forwarded to mvn for full debug output.
      set -eux
      ARGS+=("-X")
      shift
      ;;
    -h|--help)
      echo "USAGE"
      echo "    gen-proj.sh [--group-id] [--artifact-id] [--version] [--package] [--image-repository] [--scm-connection] [--scm-url] [-o|--output] [-v|--verbose]"
      echo "EXAMPLES"
      echo "    gen-proj.sh --group-id xyz.jiangsier --artifact-id jiangsier-archetype-demo --image-repository jiangsier/jiangsier-archetype-demo"
      exit 0
      ;;
    *)
      # Unknown args are forwarded to `mvn archetype:generate` verbatim
      # (e.g. -DinteractiveMode=false, extra -D properties).
      ARGS+=("$1")
      shift
      ;;
  esac
done

if [[ -z "${group_id}" ]]; then
  echo "Please specify group id."
  exit 1
fi

if [[ -z "${artifact_id}" ]]; then
  echo "Please specify artifact id."
  exit 1
fi

# --- derive defaults ---------------------------------------------------------
# All conventions assume a Java-style groupId and a GitHub-hosted SCM. Override
# any of these via flags if your layout differs.

if [[ -z "${package}" ]]; then
  package=${group_id}
fi

# Maven's archetype plugin needs the package as a path (a/b/c) for some
# resource lookups in addition to the dotted form.
package_dir=${package//./\/}

if [[ -z "${image_repository}" ]]; then
  image_repository=${group_id}/${artifact_id}
fi

if [[ -z "${scm_connection}" ]]; then
  scm_connection=scm:git:https://github.com/$(to_namespace ${group_id})/${artifact_id}.git
fi

if [[ -z "${scm_url}" ]]; then
  scm_url=https://github.com/$(to_namespace ${group_id})/${artifact_id}
fi

if [[ -z "${domain}" ]]; then
  domain=$(to_domain ${group_id})
fi

# Seed archetype-catalog.xml in both locations Maven may consult. Different
# Maven versions / configurations look in ~/.m2 vs ~/.m2/repository; copying to
# both avoids "archetype not found" errors on a fresh machine. Only seeded if
# absent — we don't clobber a user's existing catalog.
if [[ ! -f "${m2home}/archetype-catalog.xml" ]]; then
  cp -f "${project_path}/archetype-catalog.xml" "${m2home}/"
fi

if [[ ! -f "${m2home}/repository/archetype-catalog.xml" ]]; then
  cp -f "${project_path}/archetype-catalog.xml" "${m2home}/repository/"
fi

# Install this archetype into the local repo, then register it in the local
# catalog so the generate step below can resolve it via -DarchetypeCatalog=local
# without hitting the network.
mvn -f "${project_path}/pom.xml" clean install archetype:update-local-catalog

if [[ -n "${output}" ]]; then
  if [[ ! -d "${output}" ]]; then
    mkdir -p "${output}"
  fi
  cd "${output}"
fi

# Destructive: any pre-existing directory matching the new artifactId is wiped
# so `archetype:generate` can write into a clean target. Callers (e.g. a skill)
# should `--output` to a scratch dir to keep this safe.
if [[ -d "${artifact_id}" ]]; then
  rm -rf "${artifact_id}"
fi

# -B = batch mode (non-interactive); required for scripted use because
# archetype:generate prompts for missing properties otherwise.
mvn archetype:generate -B \
  -DarchetypeGroupId=${archetype_group_id} \
  -DarchetypeArtifactId=${archetype_artifact_id} \
  -DarchetypeVersion=${archetype_version} \
  -DarchetypeCatalog=local \
  -DgroupId=${group_id} \
  -DartifactId=${artifact_id} \
  -Dversion=${version} \
  -Dpackage=${package} \
  -DpackageDir=${package_dir} \
  -DimageRepository=${image_repository} \
  -DappDomain=${domain} \
  -DscmConnection=${scm_connection} \
  -DscmUrl=${scm_url} \
  ${ARGS[*]}
