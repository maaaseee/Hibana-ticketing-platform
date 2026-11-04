<#
.SYNOPSIS
    Automated End-to-End Azure Infrastructure & AKS Deployment Script for Hibana.tk
.DESCRIPTION
    1. Provisions Azure PaaS & AKS infrastructure via Bicep (infra/bicep/main.bicep).
    2. Builds & pushes backend (Java 25 / Spring Boot 4) and frontend (Vue 3) images to ACR.
    3. Uploads initial venue seatmap JSON files to Azure Blob Storage.
    4. Hydrates Kubernetes manifests with Bicep outputs and deploys to AKS.
#>
param(
    [string]$ResourceGroupName = "rg-hibana-prod",
    [string]$Location = "eastus",
    [string]$EnvironmentName = "prod",
    [Parameter(Mandatory=$true)][string]$PostgresPassword,
    [Parameter(Mandatory=$true)][string]$WaitingRoomJwtSecret,
    [Parameter(Mandatory=$true)][string]$GatewayHmacSecret
)

$ErrorActionPreference = "Stop"

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host " Hibana.tk - Azure AKS Cloud-Native Deployment Pipeline" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

# 1. Ensure Resource Group exists
Write-Host "[1/6] Creating Resource Group '$ResourceGroupName' in '$Location'..." -ForegroundColor Yellow
az group create --name $ResourceGroupName --location $Location --output none

# 2. Deploy Bicep Infrastructure
Write-Host "[2/6] Deploying Azure Infrastructure via Bicep (AKS, PostgreSQL 17, Redis, Service Bus, Key Vault, ACR, Storage)..." -ForegroundColor Yellow
$deploymentJson = az deployment group create `
    --resource-group $ResourceGroupName `
    --template-file "infra/bicep/main.bicep" `
    --parameters `
        environmentName=$EnvironmentName `
        postgresAdminPassword=$PostgresPassword `
        waitingRoomJwtSecret=$WaitingRoomJwtSecret `
        gatewayHmacSecret=$GatewayHmacSecret `
    --query "properties.outputs" `
    --output json | ConvertFrom-Json

$aksClusterName       = $deploymentJson.aksClusterName.value
$acrLoginServer       = $deploymentJson.acrLoginServer.value
$acrName              = $acrLoginServer.Split('.')[0]
$keyVaultName         = $deploymentJson.keyVaultName.value
$keyVaultUri          = $deploymentJson.keyVaultUri.value
$workloadClientId     = $deploymentJson.managedIdentityClientId.value
$postgresFqdn         = $deploymentJson.postgresFqdn.value
$redisHostName        = $deploymentJson.redisHostName.value
$storageBlobEndpoint  = $deploymentJson.storageBlobEndpoint.value
$tenantId             = (az account show --query tenantId -o tsv).Trim()

Write-Host "  -> AKS Cluster:    $aksClusterName" -ForegroundColor Green
Write-Host "  -> ACR Server:     $acrLoginServer" -ForegroundColor Green
Write-Host "  -> Key Vault:      $keyVaultName" -ForegroundColor Green
Write-Host "  -> PostgreSQL:     $postgresFqdn" -ForegroundColor Green
Write-Host "  -> Redis Cache:    $redisHostName" -ForegroundColor Green

# 3. Build & Push Container Images to ACR
Write-Host "[3/6] Building and pushing microservice & frontend images to ACR ($acrName)..." -ForegroundColor Yellow
az acr build --registry $acrName --image "ticketing-gateway:latest" ./ticketing-app-gateway
az acr build --registry $acrName --image "ticketing-catalog:latest" ./ticketing-app-catalog
az acr build --registry $acrName --image "ticketing-inventory:latest" ./ticketing-app-inventory
az acr build --registry $acrName --image "ticketing-orders:latest" ./ticketing-app-orders
az acr build --registry $acrName --image "ticketing-frontend:latest" --file ./ticketing-frontend/Dockerfile.prod ./ticketing-frontend
az acr build --registry $acrName --image "ticketing-admin:latest" --file ./ticketing-admin/Dockerfile.prod ./ticketing-admin

# 4. Seed Venue Seat Map JSON files to Azure Blob Storage
Write-Host "[4/6] Uploading venue seat map layouts (.json) to Azure Blob Storage..." -ForegroundColor Yellow
$storageAccountName = ($storageBlobEndpoint -replace "https://", "").Split('.')[0]
Get-ChildItem -Path "infra/local-dev/seat-maps/*.json" | ForEach-Object {
    az storage blob upload `
        --account-name $storageAccountName `
        --container-name "venues" `
        --name "seatmaps/$($_.Name)" `
        --file $_.FullName `
        --auth-mode login `
        --overwrite true `
        --output none
}

# 5. Fetch AKS Credentials
Write-Host "[5/6] Configuring kubectl context for '$aksClusterName'..." -ForegroundColor Yellow
az aks get-credentials --resource-group $ResourceGroupName --name $aksClusterName --overwrite-existing

# 6. Hydrate & Apply Kubernetes Manifests
Write-Host "[6/6] Hydrating and applying Kubernetes manifests to AKS..." -ForegroundColor Yellow
$tempDir = Join-Path $env:TEMP "hibana-k8s-hydrated"
if (Test-Path $tempDir) { Remove-Item $tempDir -Recurse -Force }
Copy-Item -Path "infra/k8s" -Destination $tempDir -Recurse

Get-ChildItem -Path $tempDir -Filter "*.yaml" | ForEach-Object {
    $content = Get-Content $_.FullName -Raw
    $content = $content.Replace('${AZURE_ACR_LOGIN_SERVER}', $acrLoginServer)
    $content = $content.Replace('${AZURE_WORKLOAD_IDENTITY_CLIENT_ID}', $workloadClientId)
    $content = $content.Replace('${AZURE_KEYVAULT_NAME}', $keyVaultName)
    $content = $content.Replace('${AZURE_KEYVAULT_URI}', $keyVaultUri)
    $content = $content.Replace('${AZURE_TENANT_ID}', $tenantId)
    $content = $content.Replace('${AZURE_POSTGRES_FQDN}', $postgresFqdn)
    $content = $content.Replace('${AZURE_REDIS_HOSTNAME}', $redisHostName)
    $content = $content.Replace('${AZURE_STORAGE_BLOB_ENDPOINT}', $storageBlobEndpoint)
    Set-Content -Path $_.FullName -Value $content -Encoding UTF8
}

kubectl apply -k $tempDir
Remove-Item $tempDir -Recurse -Force

Write-Host "==========================================================" -ForegroundColor Green
Write-Host " Deployment Complete! Verify pods with:" -ForegroundColor Green
Write-Host "   kubectl get pods -n hibana-prod" -ForegroundColor White
Write-Host "==========================================================" -ForegroundColor Green
