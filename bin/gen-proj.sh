#!/bin/bash

project_path=$(cd $(dirname ${BASH_SOURCE[0]})/..; pwd)
archetype_group_id=xyz.jiangsier
archetype_artifact_id=jiangsier-archetype-maven
archetype_version=1.0.0
group_id=
artifact_id=
version=1.0.0-SNAPSHOT
image_repository=
scm_connection=
scm_url=
domain=
output=
home=${HOME:-~}
m2home=${home}/.m2

function to_domain {
  arr=($(echo ${1//./ }))
  domain_by_group_id=$(printf '%s\n' "${arr[@]}" | tac | tr '\n' '.')
  echo ${domain_by_group_id%.}
}

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
      set -eux
      ARGS+=("-X")
      shift
      ;;
    -h|--help)
      echo "USAGE"
      echo "    gen-proj.sh [--group-id] [--artifact-id] [--version] [--image-repository] [--scm-connection] [--scm-url] [-o|--output] [-v|--verbose]"
      echo "EXAMPLES"
      echo "    gen-proj.sh --group-id xyz.jiangsier --artifact-id jiangsier-archetype-demo --image-repository jiangsier/jiangsier-archetype-demo"
      exit 0
      ;;
    *)
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

if [[ ! -f "${m2home}/archetype-catalog.xml" ]]; then
  cp -f "${project_path}/archetype-catalog.xml" "${m2home}/"
fi

if [[ ! -f "${m2home}/repository/archetype-catalog.xml" ]]; then
  cp -f "${project_path}/archetype-catalog.xml" "${m2home}/repository/"
fi

mvn -f "${project_path}/pom.xml" clean install archetype:update-local-catalog

if [[ -n "${output}" ]]; then
  if [[ ! -d "${output}" ]]; then
    mkdir -p "${output}"
  fi
  cd "${output}"
fi

if [[ -d "${artifact_id}" ]]; then
  rm -rf "${artifact_id}"
fi

mvn archetype:generate -B \
  -DarchetypeGroupId=${archetype_group_id} \
  -DarchetypeArtifactId=${archetype_artifact_id} \
  -DarchetypeVersion=${archetype_version} \
  -DarchetypeCatalog=local \
  -DgroupId=${group_id} \
  -DartifactId=${artifact_id} \
  -Dversion=${version} \
  -DimageRepository=${image_repository} \
  -DappDomain=${domain} \
  -DscmConnection=${scm_connection} \
  -DscmUrl=${scm_url} \
  ${ARGS[*]}
