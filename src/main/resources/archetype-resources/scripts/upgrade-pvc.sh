#!/usr/bin/env bash

source $(dirname ${BASH_SOURCE[0]})/setenv.sh

check_helm

helm upgrade --kubeconfig ${KUBE_CONFIG} --namespace ${NAMESPACE} --create-namespace -f ${values_yaml} \
  --set mysql.enabled=false \
  --set redis.enabled=false \
  --set backend.enabled=false \
  --set persistence.enabled=true \
  ${ARGS[*]} \
  ${PROJECT_NAME}-pvc ${HELM_CONFIG_HOME}
