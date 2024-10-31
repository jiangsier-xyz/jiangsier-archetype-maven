#!/usr/bin/env bash

source $(dirname ${BASH_SOURCE[0]})/setenv.sh

check_docker
check_helm

COMPOSE_CONFIG=$(mktemp -d)/build.yml

cd ${SCRIPTS_PATH}

mvn clean package -Dmaven.test.skip=true -f ${PROJECT_PATH}/pom.xml;ret=$?
test ${ret} -eq 0 || die "ERROR: Failed to build ${PROJECT_NAME}!"

cp -f ${PROJECT_PATH}/${STARTER_MODULE}/target/${STARTER_MODULE}-${VERSION}.jar ${DOCKER_CONFIG_HOME}/${PROJECT_NAME}.jar

# compose config
cat > ${COMPOSE_CONFIG} <<EOF
services:
  ${PROJECT_NAME}:
    build:
      context: ${DOCKER_CONFIG_HOME}
      dockerfile: Dockerfile
      args:
        - APP_NAME=${PROJECT_NAME}
        - UNAME=${HELM_backend_systemAccount:-admin}
        - UID=${HELM_backend_securityContext_runAsUser:-10000}
        - GID=${HELM_backend_securityContext_runAsGroup:-10000}
      tags:
        - ${HELM_backend_image_repository}:${VERSION}
      platforms:
        - linux/amd64
    image: ${HELM_backend_image_repository}:latest
EOF

if [[ "${VERBOSE}" == "1" ]];then
  echo "[COMPOSE CONFIG]"
  cat ${COMPOSE_CONFIG}
fi

docker compose -f ${COMPOSE_CONFIG} -p ${PROJECT_NAME} build --push ${PROJECT_NAME}
helm dependency update ${HELM_CONFIG_HOME}

rm -f ${DOCKER_CONFIG_HOME}/${PROJECT_NAME}.jar
