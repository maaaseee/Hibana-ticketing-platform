@description('Azure deployment region for Hibana.tk Ticketing Platform')
param location string = resourceGroup().location

@description('Environment name (e.g. prod, staging, dev)')
@allowed(['prod', 'staging', 'dev'])
param environmentName string = 'prod'

@description('Name of the Key Vault (must be globally unique)')
param keyVaultName string = 'kv-hibana-${environmentName}-${uniqueString(resourceGroup().id)}'

@description('AKS OIDC Issuer URL for Entra Workload Identity federation')
param aksOidcIssuerUrl string = 'https://eastus.oic.prod-aks.azure.com/00000000-0000-0000-0000-000000000000/00000000-0000-0000-0000-000000000001/'

@description('Kubernetes namespace where backend services run')
param k8sNamespace string = 'hibana-${environmentName}'

@description('Kubernetes service account name for workload identity')
param k8sServiceAccountName string = 'hibana-backend-sa'

// -----------------------------------------------------------------------------
// 1. User Assigned Managed Identity (Workload Identity)
// -----------------------------------------------------------------------------
resource backendManagedIdentity 'Microsoft.ManagedIdentity/userAssignedIdentities@2023-01-31' = {
  name: 'id-hibana-backend-${environmentName}'
  location: location
  tags: {
    Environment: environmentName
    Application: 'Hibana.tk'
    Component: 'Backend-Workload-Identity'
  }
}

// -----------------------------------------------------------------------------
// 2. Federated Identity Credential (AKS OIDC -> Entra ID)
// -----------------------------------------------------------------------------
resource federatedCredential 'Microsoft.ManagedIdentity/userAssignedIdentities/federatedIdentityCredentials@2023-01-31' = {
  name: 'fic-hibana-backend-${environmentName}'
  parent: backendManagedIdentity
  properties: {
    issuer: aksOidcIssuerUrl
    subject: 'system:serviceaccount:${k8sNamespace}:${k8sServiceAccountName}'
    audiences: [
      'api://AzureADTokenExchange'
    ]
  }
}

// -----------------------------------------------------------------------------
// 3. Azure Key Vault with Azure RBAC Authorization
// -----------------------------------------------------------------------------
resource keyVault 'Microsoft.KeyVault/vaults@2023-07-01' = {
  name: keyVaultName
  location: location
  properties: {
    tenantId: subscription().tenantId
    sku: {
      family: 'A'
      name: 'standard'
    }
    enableRbacAuthorization: true
    enableSoftDelete: true
    softDeleteRetentionInDays: 90
    enablePurgeProtection: true
    networkAcls: {
      bypass: 'AzureServices'
      defaultAction: 'Deny'
    }
  }
  tags: {
    Environment: environmentName
    Application: 'Hibana.tk'
    SecurityClassification: 'Confidential'
  }
}

// -----------------------------------------------------------------------------
// 4. Role Assignment: Key Vault Secrets User (4633458b-17de-408a-b874-0445c86b69e6)
// -----------------------------------------------------------------------------
var keyVaultSecretsUserRoleId = '4633458b-17de-408a-b874-0445c86b69e6'

resource roleAssignmentSecretsUser 'Microsoft.Authorization/roleAssignments@2022-04-01' = {
  name: guid(keyVault.id, backendManagedIdentity.id, keyVaultSecretsUserRoleId)
  scope: keyVault
  properties: {
    roleDefinitionId: subscriptionResourceId('Microsoft.Authorization/roleDefinitions', keyVaultSecretsUserRoleId)
    principalId: backendManagedIdentity.properties.principalId
    principalType: 'ServicePrincipal'
  }
}

// -----------------------------------------------------------------------------
// 5. Secret Placeholders in Key Vault
// -----------------------------------------------------------------------------
resource secretWaitingRoom 'Microsoft.KeyVault/vaults/secrets@2023-07-01' = {
  parent: keyVault
  name: 'waiting-room-jwt-secret'
  properties: {
    contentType: 'text/plain'
    value: 'REPLACE_WITH_STRONG_256BIT_SECRET'
  }
}

resource secretGatewayHmac 'Microsoft.KeyVault/vaults/secrets@2023-07-01' = {
  parent: keyVault
  name: 'gateway-hmac-secret'
  properties: {
    contentType: 'text/plain'
    value: 'REPLACE_WITH_STRONG_256BIT_SECRET'
  }
}

resource secretMercadoPago 'Microsoft.KeyVault/vaults/secrets@2023-07-01' = {
  parent: keyVault
  name: 'mercadopago-webhook-secret'
  properties: {
    contentType: 'text/plain'
    value: 'REPLACE_WITH_MERCADOPAGO_WEBHOOK_KEY'
  }
}

resource secretDbPassword 'Microsoft.KeyVault/vaults/secrets@2023-07-01' = {
  parent: keyVault
  name: 'postgres-admin-password'
  properties: {
    contentType: 'text/plain'
    value: 'REPLACE_WITH_POSTGRES_SECURE_PASSWORD'
  }
}

resource secretRedisPassword 'Microsoft.KeyVault/vaults/secrets@2023-07-01' = {
  parent: keyVault
  name: 'redis-password'
  properties: {
    contentType: 'text/plain'
    value: 'REPLACE_WITH_REDIS_SECURE_PASSWORD'
  }
}

// -----------------------------------------------------------------------------
// Outputs
// -----------------------------------------------------------------------------
output keyVaultName string = keyVault.name
output keyVaultUri string = keyVault.properties.vaultUri
output managedIdentityClientId string = backendManagedIdentity.properties.clientId
output managedIdentityPrincipalId string = backendManagedIdentity.properties.principalId
output managedIdentityId string = backendManagedIdentity.id
