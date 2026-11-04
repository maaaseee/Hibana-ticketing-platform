#!/usr/bin/env bash
set -euo pipefail

RESOURCE_GROUP="${RESOURCE_GROUP:-rg-hibana-prod}"
LOCATION="${LOCATION:-eastus}"
ENVIRONMENT="${ENVIRONMENT:-prod}"

if [[ -z "${HIBANA_POSTGRES_PASSWORD:-}" || -z "${HIBANA_WAITING_ROOM_JWT_SECRET:-}" || -z "${HIBANA_GATEWAY_HMAC_SECRET:-}" ]]; then
  echo "Error: Required secret environment variables are missing:"
  echo "  export HIBANA_POSTGRES_PASSWORD=..."
  echo "  export HIBANA_WAITING_ROOM_JWT_SECRET=..."
  echo "  export HIBANA_GATEWAY_HMAC_SECRET=..."
  exit 1
fi

echo "[1/6] Creating Resource Group '${RESOURCE_GROUP}' in '${LOCATION}'..."
az group create --name "${RESOURCE_GROUP}" --location "${LOCATION}" --output none

echo "[2/6] Deploying Azure Infrastructure via Bicep..."
OUTPUTS=$(az deployment group create \
  --resource-group "${RESOURCE_GROUP}" \
  --template-file infra/bicep/main.bicep \
  --parameters \
    environmentName="${ENVIRONMENT}" \
    postgresAdminPassword="${HIBANA_POSTGRES_PASSWORD}" \
    waitingRoomJwtSecret="${HIBANA_WAITING_ROOM_JWT_SECRET}" \
    gatewayHmacSecret="${HIBANA_GATEWAY_HMAC_SECRET}" \
  --query "properties.outputs" \
  --output json)

export AZURE_AKS_CLUSTER_NAME=$(echo "${OUTPUTS}" | jq -r '.aksClusterName.value')
export AZURE_ACR_LOGIN_SERVER=$(echo "${OUTPUTS}" | jq -r '.acrLoginServer.value')
export ACR_NAME="${AZURE_ACR_LOGIN_SERVER%%.*}"
export AZURE_KEYVAULT_NAME=$(echo "${OUTPUTS}" | jq -r '.keyVaultName.value')
export AZURE_KEYVAULT_URI=$(echo "${OUTPUTS}" | jq -r '.keyVaultUri.value')
export AZURE_WORKLOAD_IDENTITY_CLIENT_ID=$(echo "${OUTPUTS}" | jq -r '.managedIdentityClientId.value')
export AZURE_POSTGRES_FQDN=$(echo "${OUTPUTS}" | jq -r '.postgresFqdn.value')
export AZURE_REDIS_HOSTNAME=$(echo "${OUTPUTS}" | jq -r '.redisHostName.value')
export AZURE_STORAGE_BLOB_ENDPOINT=$(echo "${OUTPUTS}" | jq -r '.storageBlobEndpoint.value')
export AZURE_TENANT_ID=$(az account show --query tenantId -o tsv)
export AZURE_PUBLIC_ORIGIN="${AZURE_PUBLIC_ORIGIN:-http://hibana-${ENVIRONMENT}.${LOCATION}.cloudapp.azure.com}"
export CORS_ALLOWED_ORIGINS="${CORS_ALLOWED_ORIGINS:-${AZURE_PUBLIC_ORIGIN}}"

echo "[3/6] Building and pushing container images to ACR (${ACR_NAME})..."
az acr build --registry "${ACR_NAME}" --image "ticketing-gateway:latest" ./ticketing-app-gateway
az acr build --registry "${ACR_NAME}" --image "ticketing-catalog:latest" ./ticketing-app-catalog
az acr build --registry "${ACR_NAME}" --image "ticketing-inventory:latest" ./ticketing-app-inventory
az acr build --registry "${ACR_NAME}" --image "ticketing-orders:latest" ./ticketing-app-orders
az acr build --registry "${ACR_NAME}" --image "ticketing-frontend:latest" --file ./ticketing-frontend/Dockerfile.prod ./ticketing-frontend
az acr build --registry "${ACR_NAME}" --image "ticketing-admin:latest" --file ./ticketing-admin/Dockerfile.prod ./ticketing-admin

echo "[4/6] Configuring kubectl credentials for '${AZURE_AKS_CLUSTER_NAME}'..."
az aks get-credentials --resource-group "${RESOURCE_GROUP}" --name "${AZURE_AKS_CLUSTER_NAME}" --overwrite-existing

echo "[5/6] Hydrating and applying Kubernetes manifests..."
TMP_DIR=$(mktemp -d)
cp -r infra/k8s/* "${TMP_DIR}/"

for f in "${TMP_DIR}"/*.yaml; do
  envsubst '${AZURE_ACR_LOGIN_SERVER} ${AZURE_WORKLOAD_IDENTITY_CLIENT_ID} ${AZURE_KEYVAULT_NAME} ${AZURE_KEYVAULT_URI} ${AZURE_TENANT_ID} ${AZURE_POSTGRES_FQDN} ${AZURE_REDIS_HOSTNAME} ${AZURE_STORAGE_BLOB_ENDPOINT} ${AZURE_PUBLIC_ORIGIN} ${CORS_ALLOWED_ORIGINS}' < "$f" > "$f.tmp"
  mv "$f.tmp" "$f"
done

kubectl apply -k "${TMP_DIR}"
rm -rf "${TMP_DIR}"

echo "[6/6] Deployment complete! Check rollout status with: kubectl get pods -n hibana-prod"
