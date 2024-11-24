#!/usr/bin/env bash

source $(dirname ${BASH_SOURCE[0]})/setenv.sh

check_helm

helm upgrade --kubeconfig ${KUBE_CONFIG} --namespace ${NAMESPACE} -f ${values_yaml} \
  --set-file mysql.initdbScripts.1-schema\\.sql=${PROJECT_PATH}/${DAL_MODULE}/src/main/resources/sql/schema.sql \
  --set backend.enabled=true \
  --set persistence.enabled=false \
  ${ARGS[*]} \
  ${PROJECT_NAME} ${HELM_CONFIG_HOME}
