<#
.SYNOPSIS
  Deploys Hibana.tk Azure Infrastructure (Bicep) and Kubernetes Manifests (Kustomize)
  supporting both Production ('prod') and Single-Node Load-Testing ('dev') workflows.

.EXAMPLE
  # 1. Dry-run (What-If) for dev/test (1x 4-vCPU node for k6 load testing):
  .\infra\deploy-azure.ps1 -Environment dev -WhatIf

  # 2. Full deployment for dev/test:
  .\infra\deploy-azure.ps1 -Environment dev

  # 3. Full deployment for production (Multi-pool HA):
  .\infra\deploy-azure.ps1 -Environment prod
#>
param(
    [ValidateSet('prod', 'staging', 'dev')]
    [string]$Environment = 'dev',

    [string]$Location = 'eastus2',

    [string]$ResourceGroup = "rg-hibana-$Environment",

    [switch]$WhatIf,

    [switch]$SkipImageBuild
)

$ErrorActionPreference = 'Stop'

$K8sNamespace = "hibana-$Environment"
$BicepParamFile = if ($Environment -eq 'prod') { 'infra/bicep/main.bicepparam' } else { 'infra/bicep/dev.bicepparam' }
$KustomizeMode = if ($Environment -eq 'prod') { 'kustomization.yaml (prod)' } else { 'kustomization.dev.yaml (single-node k6 profile)' }

# Ensure default secrets exist in environment if not already set by user
if (-not $env:HIBANA_POSTGRES_PASSWORD)        { $env:HIBANA_POSTGRES_PASSWORD        = 'Hibana_StrongPsqlPass_2026!' }
if (-not $env:HIBANA_WAITING_ROOM_JWT_SECRET) { $env:HIBANA_WAITING_ROOM_JWT_SECRET = 'Hibana_WaitingRoomJwtSecret_256Bit_2026_SecureKey!' }
if (-not $env:HIBANA_GATEWAY_HMAC_SECRET)     { $env:HIBANA_GATEWAY_HMAC_SECRET     = 'Hibana_GatewayInternalHmacSecret_256Bit_2026_Key!' }
if (-not $env:HIBANA_MP_WEBHOOK_SECRET)       { $env:HIBANA_MP_WEBHOOK_SECRET       = 'Hibana_MercadoPagoWebhookSecret_2026!' }

Write-Host "[1/6] Ensuring Resource Group '$ResourceGroup' in '$Location'..." -ForegroundColor Cyan
az group create --name $ResourceGroup --location $Location --output none

if ($WhatIf) {
    Write-Host "[2/6] Running Azure Bicep What-If Simulation ($BicepParamFile)..." -ForegroundColor Yellow
    az deployment group what-if `
        --resource-group $ResourceGroup `
        --template-file infra/bicep/main.bicep `
        --parameters $BicepParamFile `
        --parameters environmentName=$Environment k8sNamespace=$K8sNamespace
    Write-Host "What-If complete. Re-run without -WhatIf to provision resources." -ForegroundColor Green
    return
}

Write-Host "[2/6] Deploying Azure Infrastructure via Bicep ($BicepParamFile)..." -ForegroundColor Cyan
$OutputsJson = az deployment group create `
    --resource-group $ResourceGroup `
    --template-file infra/bicep/main.bicep `
    --parameters $BicepParamFile `
    --parameters environmentName=$Environment k8sNamespace=$K8sNamespace `
    --query "properties.outputs" `
    --output json

if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($OutputsJson)) {
    throw "Bicep deployment failed (exit code $LASTEXITCODE). Aborting script before ACR/Kubernetes steps."
}

$Outputs = $OutputsJson | ConvertFrom-Json

$AksClusterName            = $Outputs.aksClusterName.value
$AcrLoginServer            = $Outputs.acrLoginServer.value
$AcrName                   = ($AcrLoginServer -split '\.')[0]
$KeyVaultName              = $Outputs.keyVaultName.value
$KeyVaultUri               = $Outputs.keyVaultUri.value
$WorkloadIdentityClientId  = $Outputs.managedIdentityClientId.value
$PostgresFqdn              = $Outputs.postgresFqdn.value
$RedisHostName             = $Outputs.redisHostName.value
$StorageBlobEndpoint       = $Outputs.storageBlobEndpoint.value
$TenantId                  = (az account show --query tenantId -o tsv).Trim()
$PublicOrigin              = if ($env:AZURE_PUBLIC_ORIGIN) { $env:AZURE_PUBLIC_ORIGIN } else { "http://hibana-$Environment.$Location.cloudapp.azure.com" }
$CorsAllowedOrigins        = if ($env:CORS_ALLOWED_ORIGINS) { $env:CORS_ALLOWED_ORIGINS } else { $PublicOrigin }

if (-not $SkipImageBuild -and $Environment -eq 'prod') {
    Write-Host "[3/6] Building and pushing container images to ACR ($AcrName)..." -ForegroundColor Cyan
    az acr build --registry $AcrName --image "ticketing-gateway:latest" ./ticketing-app-gateway
    az acr build --registry $AcrName --image "ticketing-catalog:latest" ./ticketing-app-catalog
    az acr build --registry $AcrName --image "ticketing-inventory:latest" ./ticketing-app-inventory
    az acr build --registry $AcrName --image "ticketing-orders:latest" ./ticketing-app-orders
    az acr build --registry $AcrName --image "ticketing-frontend:latest" --file ./ticketing-frontend/Dockerfile.prod ./ticketing-frontend
    az acr build --registry $AcrName --image "ticketing-admin:latest" --file ./ticketing-admin/Dockerfile.prod ./ticketing-admin
} else {
    Write-Host "[3/6] Using pre-built cloud images from Container Registry ($AcrLoginServer)." -ForegroundColor Yellow
}

Write-Host "[4/6] Configuring kubectl credentials for '$AksClusterName'..." -ForegroundColor Cyan
az aks get-credentials --resource-group $ResourceGroup --name $AksClusterName --overwrite-existing
if ($LASTEXITCODE -ne 0) {
    throw "Failed to get AKS credentials for '$AksClusterName'. Aborting kubectl apply."
}

Write-Host "[5/6] Hydrating and applying Kubernetes manifests ($KustomizeMode)..." -ForegroundColor Cyan
$TmpDir = Join-Path ([IO.Path]::GetTempPath()) ("hibana-k8s-" + [Guid]::NewGuid().ToString("N"))
Copy-Item -Path "infra/k8s" -Destination $TmpDir -Recurse

if ($Environment -ne 'prod') {
    Move-Item -Path (Join-Path $TmpDir "kustomization.dev.yaml") -Destination (Join-Path $TmpDir "kustomization.yaml") -Force
}

$Replacements = @{
    '${AZURE_ACR_LOGIN_SERVER}'            = $AcrLoginServer
    '${AZURE_WORKLOAD_IDENTITY_CLIENT_ID}' = $WorkloadIdentityClientId
    '${AZURE_KEYVAULT_NAME}'               = $KeyVaultName
    '${AZURE_KEYVAULT_URI}'                = $KeyVaultUri
    '${AZURE_TENANT_ID}'                   = $TenantId
    '${AZURE_POSTGRES_FQDN}'               = $PostgresFqdn
    '${AZURE_REDIS_HOSTNAME}'              = $RedisHostName
    '${AZURE_STORAGE_BLOB_ENDPOINT}'       = $StorageBlobEndpoint
    '${AZURE_PUBLIC_ORIGIN}'               = $PublicOrigin
    '${CORS_ALLOWED_ORIGINS}'              = $CorsAllowedOrigins
}

Get-ChildItem -Path $TmpDir -Filter *.yaml -Recurse | ForEach-Object {
    $Content = [IO.File]::ReadAllText($_.FullName)
    foreach ($Key in $Replacements.Keys) {
        $Content = $Content.Replace($Key, $Replacements[$Key])
    }
    [IO.File]::WriteAllText($_.FullName, $Content, (New-Object Text.UTF8Encoding $false))
}

kubectl apply -k $TmpDir
Remove-Item -Path $TmpDir -Recurse -Force

Write-Host "[6/6] Deployment complete! Check rollout status with: kubectl get pods -n $K8sNamespace" -ForegroundColor Green
