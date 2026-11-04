@description('Azure deployment region for Hibana.tk Ticketing Platform')
param location string = resourceGroup().location

@description('Environment name (e.g. prod, staging, dev)')
@allowed(['prod', 'staging', 'dev'])
param environmentName string = 'prod'

@description('Kubernetes namespace where backend services run')
param k8sNamespace string = 'hibana-${environmentName}'

@description('Kubernetes service account name for workload identity')
param k8sServiceAccountName string = 'hibana-backend-sa'

@description('PostgreSQL administrator login username')
param postgresAdminLogin string = 'ticketing_admin'

@secure()
@description('PostgreSQL administrator password')
param postgresAdminPassword string

@secure()
@description('256-bit HMAC secret for Virtual Waiting Room Checkout JWT tokens')
param waitingRoomJwtSecret string

@secure()
@description('256-bit HMAC secret for Gateway internal stamp verification')
param gatewayHmacSecret string

@secure()
@description('Mercado Pago Webhook signature verification secret')
param mercadoPagoWebhookSecret string = ''

@description('AKS VM SKU for application node pool')
param aksNodeVmSize string = 'Standard_D4s_v6'

var uniqueSuffix = uniqueString(resourceGroup().id, environmentName)
var keyVaultName = take('kv-hibana-${environmentName}-${uniqueSuffix}', 24)
var acrName = take('crhibana${environmentName}${uniqueSuffix}', 50)
var storageAccountName = take('sthibana${environmentName}${uniqueSuffix}', 24)
var postgresServerName = 'psql-hibana-${environmentName}-${uniqueSuffix}'
var redisCacheName = 'redis-hibana-${environmentName}-${uniqueSuffix}'
var serviceBusNamespaceName = 'sb-hibana-${environmentName}-${uniqueSuffix}'
var aksClusterName = 'aks-hibana-${environmentName}'
var logAnalyticsName = 'log-hibana-${environmentName}'
var appInsightsName = 'appi-hibana-${environmentName}'
var vnetName = 'vnet-hibana-${environmentName}'

var commonTags = {
  Environment: environmentName
  Application: 'Hibana.tk'
  ManagedBy: 'Azure-Bicep-IaC'
}

// =============================================================================
// 1. Virtual Network & Subnets (10.0.0.0/16) — Diagram 08 Topology
// =============================================================================
resource nsgIngress 'Microsoft.Network/networkSecurityGroups@2023-11-01' = {
  name: 'nsg-hibana-ingress-${environmentName}'
  location: location
  tags: commonTags
  properties: {
    securityRules: [
      {
        name: 'Allow-HTTPS-Inbound'
        properties: {
          priority: 100
          protocol: 'Tcp'
          access: 'Allow'
          direction: 'Inbound'
          sourceAddressPrefix: 'Internet'
          sourcePortRange: '*'
          destinationAddressPrefix: '*'
          destinationPortRange: '443'
        }
      }
      {
        name: 'Allow-HTTP-Inbound'
        properties: {
          priority: 110
          protocol: 'Tcp'
          access: 'Allow'
          direction: 'Inbound'
          sourceAddressPrefix: 'Internet'
          sourcePortRange: '*'
          destinationAddressPrefix: '*'
          destinationPortRange: '80'
        }
      }
    ]
  }
}

resource nsgData 'Microsoft.Network/networkSecurityGroups@2023-11-01' = {
  name: 'nsg-hibana-data-${environmentName}'
  location: location
  tags: commonTags
  properties: {
    securityRules: [
      {
        name: 'Allow-AKS-Subnet-To-Data'
        properties: {
          priority: 100
          protocol: 'Tcp'
          access: 'Allow'
          direction: 'Inbound'
          sourceAddressPrefix: '10.0.2.0/23'
          sourcePortRange: '*'
          destinationAddressPrefix: '10.0.4.0/24'
          destinationPortRanges: [
            '5432'
            '6380'
            '5671'
            '5672'
            '443'
          ]
        }
      }
    ]
  }
}

resource vnet 'Microsoft.Network/virtualNetworks@2023-11-01' = {
  name: vnetName
  location: location
  tags: commonTags
  properties: {
    addressSpace: {
      addressPrefixes: [
        '10.0.0.0/16'
      ]
    }
    subnets: [
      {
        name: 'snet-ingress'
        properties: {
          addressPrefix: '10.0.1.0/24'
          networkSecurityGroup: {
            id: nsgIngress.id
          }
        }
      }
      {
        name: 'snet-aks'
        properties: {
          addressPrefix: '10.0.2.0/23'
        }
      }
      {
        name: 'snet-data'
        properties: {
          addressPrefix: '10.0.4.0/24'
          networkSecurityGroup: {
            id: nsgData.id
          }
        }
      }
    ]
  }
}

// =============================================================================
// 2. Observability: Log Analytics Workspace & Application Insights
// =============================================================================
resource logAnalytics 'Microsoft.OperationalInsights/workspaces@2023-09-01' = {
  name: logAnalyticsName
  location: location
  tags: commonTags
  properties: {
    sku: {
      name: 'PerGB2018'
    }
    retentionInDays: 30
  }
}

resource appInsights 'Microsoft.Insights/components@2020-02-02' = {
  name: appInsightsName
  location: location
  kind: 'java'
  tags: commonTags
  properties: {
    Application_Type: 'other'
    WorkspaceResourceId: logAnalytics.id
    IngestionMode: 'LogAnalytics'
  }
}

// =============================================================================
// 3. Container Registry (ACR in 'prod'; GitHub Container Registry ghcr.io in 'dev')
// =============================================================================
@description('GitHub Container Registry namespace used when environmentName != prod')
param ghcrNamespace string = 'ghcr.io/maaaseee'

resource acr 'Microsoft.ContainerRegistry/registries@2023-07-01' = if (environmentName == 'prod') {
  name: acrName
  location: location
  tags: commonTags
  sku: {
    name: environmentName == 'prod' ? 'Premium' : 'Standard'
  }
  properties: {
    adminUserEnabled: false
    publicNetworkAccess: 'Enabled'
  }
}

// =============================================================================
// 4. Azure Kubernetes Service (AKS) with Workload Identity & Key Vault CSI
// =============================================================================
resource aksCluster 'Microsoft.ContainerService/managedClusters@2024-02-01' = {
  name: aksClusterName
  location: location
  tags: commonTags
  identity: {
    type: 'SystemAssigned'
  }
  properties: {
    dnsPrefix: '${aksClusterName}-dns'
    enableRBAC: true
    oidcIssuerProfile: {
      enabled: true
    }
    securityProfile: {
      workloadIdentity: {
        enabled: true
      }
    }
    networkProfile: {
      networkPlugin: 'azure'
      networkPluginMode: 'overlay'
      networkPolicy: 'calico'
      serviceCidr: '10.240.0.0/16'
      dnsServiceIP: '10.240.0.10'
    }
    addonProfiles: {
      omsAgent: {
        enabled: true
        config: {
          logAnalyticsWorkspaceResourceID: logAnalytics.id
        }
      }
      azureKeyvaultSecretsProvider: {
        enabled: true
        config: {
          enableSecretRotation: 'true'
          rotationPollInterval: '2m'
        }
      }
    }
    agentPoolProfiles: environmentName == 'prod' ? [
      {
        name: 'systempool'
        count: 2
        minCount: 2
        maxCount: 3
        enableAutoScaling: true
        vmSize: 'Standard_D2s_v6'
        osType: 'Linux'
        osSKU: 'Ubuntu'
        mode: 'System'
        vnetSubnetID: vnet.properties.subnets[1].id
        availabilityZones: [
          '1'
          '2'
          '3'
        ]
      }
      {
        name: 'appspool'
        count: 3
        minCount: 2
        maxCount: 10
        enableAutoScaling: true
        vmSize: aksNodeVmSize
        osType: 'Linux'
        osSKU: 'Ubuntu'
        mode: 'User'
        vnetSubnetID: vnet.properties.subnets[1].id
        availabilityZones: [
          '1'
          '2'
          '3'
        ]
      }
    ] : [
      {
        name: 'systempool'
        count: 1
        enableAutoScaling: false
        vmSize: aksNodeVmSize
        maxPods: 60
        osType: 'Linux'
        osSKU: 'Ubuntu'
        mode: 'System'
        vnetSubnetID: vnet.properties.subnets[1].id
      }
    ]
  }
}

// Grant AKS Kubelet Identity AcrPull permission on ACR (only in 'prod')
var acrPullRoleId = '7f951dda-4ed3-4680-a7ca-43fe172d538d'
resource aksAcrPullRole 'Microsoft.Authorization/roleAssignments@2022-04-01' = if (environmentName == 'prod') {
  name: guid(resourceGroup().id, aksCluster.id, acrPullRoleId)
  scope: acr
  properties: {
    roleDefinitionId: subscriptionResourceId('Microsoft.Authorization/roleDefinitions', acrPullRoleId)
    principalId: aksCluster.properties.identityProfile.kubeletidentity.objectId
    principalType: 'ServicePrincipal'
  }
}

// =============================================================================
// 5. User-Assigned Managed Identity (Workload Identity) & Federated Credential
// =============================================================================
resource backendManagedIdentity 'Microsoft.ManagedIdentity/userAssignedIdentities@2023-01-31' = {
  name: 'id-hibana-backend-${environmentName}'
  location: location
  tags: union(commonTags, {
    Component: 'Backend-Workload-Identity'
  })
}

resource federatedCredential 'Microsoft.ManagedIdentity/userAssignedIdentities/federatedIdentityCredentials@2023-01-31' = {
  name: 'fic-hibana-backend-${environmentName}'
  parent: backendManagedIdentity
  properties: {
    issuer: aksCluster.properties.oidcIssuerProfile.issuerURL
    subject: 'system:serviceaccount:${k8sNamespace}:${k8sServiceAccountName}'
    audiences: [
      'api://AzureADTokenExchange'
    ]
  }
}

// =============================================================================
// 6. Azure Database for PostgreSQL Flexible Server (PostgreSQL 17 + pgvector)
//    Provisioned as managed PaaS in 'prod'; runs in-cluster on AKS in 'dev'
// =============================================================================
resource postgresServer 'Microsoft.DBforPostgreSQL/flexibleServers@2023-12-01-preview' = if (environmentName == 'prod') {
  name: postgresServerName
  location: location
  tags: commonTags
  sku: {
    name: environmentName == 'prod' ? 'Standard_D4ds_v5' : 'Standard_B2ms'
    tier: environmentName == 'prod' ? 'GeneralPurpose' : 'Burstable'
  }
  properties: {
    version: '17'
    administratorLogin: postgresAdminLogin
    administratorLoginPassword: postgresAdminPassword
    storage: {
      storageSizeGB: 128
      autoGrow: 'Enabled'
    }
    backup: {
      backupRetentionDays: 14
      geoRedundantBackup: environmentName == 'prod' ? 'Enabled' : 'Disabled'
    }
    highAvailability: {
      mode: environmentName == 'prod' ? 'ZoneRedundant' : 'Disabled'
    }
  }
}

resource postgresExtensions 'Microsoft.DBforPostgreSQL/flexibleServers/configurations@2023-12-01-preview' = if (environmentName == 'prod') {
  parent: postgresServer
  name: 'azure.extensions'
  properties: {
    value: 'VECTOR,UUID-OSSP,PG_TRGM'
    source: 'user-override'
  }
}

resource postgresDatabase 'Microsoft.DBforPostgreSQL/flexibleServers/databases@2023-12-01-preview' = if (environmentName == 'prod') {
  parent: postgresServer
  name: 'ticketing_db'
  properties: {
    charset: 'UTF8'
    collation: 'en_US.utf8'
  }
}

resource postgresAllowAzureServices 'Microsoft.DBforPostgreSQL/flexibleServers/firewallRules@2023-12-01-preview' = if (environmentName == 'prod') {
  parent: postgresServer
  name: 'AllowAllAzureServicesAndResourcesWithinAzureIps'
  properties: {
    startIpAddress: '0.0.0.0'
    endIpAddress: '0.0.0.0'
  }
}

// =============================================================================
// 7. Azure Cache for Redis (Redlock & Virtual Waiting Room ZSETs)
//    Provisioned as managed PaaS in 'prod'; runs in-cluster on AKS in 'dev'
// =============================================================================
resource redisCache 'Microsoft.Cache/redis@2023-08-01' = if (environmentName == 'prod') {
  name: redisCacheName
  location: location
  tags: commonTags
  properties: {
    sku: {
      name: environmentName == 'prod' ? 'Premium' : 'Standard'
      family: environmentName == 'prod' ? 'P' : 'C'
      capacity: 1
    }
    enableNonSslPort: false
    minimumTlsVersion: '1.2'
    redisConfiguration: {
      'maxmemory-policy': 'volatile-lru'
    }
  }
}

// =============================================================================
// 8. Azure Service Bus Namespace, Topics & Subscriptions
// =============================================================================
resource serviceBusNamespace 'Microsoft.ServiceBus/namespaces@2022-10-01-preview' = {
  name: serviceBusNamespaceName
  location: location
  tags: commonTags
  sku: {
    name: environmentName == 'prod' ? 'Premium' : 'Standard'
    tier: environmentName == 'prod' ? 'Premium' : 'Standard'
    capacity: environmentName == 'prod' ? 1 : 0
  }
  properties: {
    minimumTlsVersion: '1.2'
  }
}

// Topic 1: event.lifecycle.v1 -> Subscription: ticketing-seat-generator
resource topicEventLifecycle 'Microsoft.ServiceBus/namespaces/topics@2022-10-01-preview' = {
  parent: serviceBusNamespace
  name: 'event.lifecycle.v1'
  properties: {
    supportOrdering: true
    maxSizeInMegabytes: 1024
  }
}

resource subSeatGenerator 'Microsoft.ServiceBus/namespaces/topics/subscriptions@2022-10-01-preview' = {
  parent: topicEventLifecycle
  name: 'ticketing-seat-generator'
  properties: {
    maxDeliveryCount: 10
    deadLetteringOnMessageExpiration: true
  }
}

// Topic 2: ticket.reservation.v1 -> Subscription: orders-reservation-tracker
resource topicTicketReservation 'Microsoft.ServiceBus/namespaces/topics@2022-10-01-preview' = {
  parent: serviceBusNamespace
  name: 'ticket.reservation.v1'
  properties: {
    supportOrdering: true
    maxSizeInMegabytes: 1024
  }
}

resource subOrdersReservation 'Microsoft.ServiceBus/namespaces/topics/subscriptions@2022-10-01-preview' = {
  parent: topicTicketReservation
  name: 'orders-reservation-tracker'
  properties: {
    maxDeliveryCount: 10
    deadLetteringOnMessageExpiration: true
  }
}

// Topic 3: order.lifecycle.v1 -> Subscription: inventory-order-settlement
resource topicOrderLifecycle 'Microsoft.ServiceBus/namespaces/topics@2022-10-01-preview' = {
  parent: serviceBusNamespace
  name: 'order.lifecycle.v1'
  properties: {
    supportOrdering: true
    maxSizeInMegabytes: 1024
  }
}

resource subInventorySettlement 'Microsoft.ServiceBus/namespaces/topics/subscriptions@2022-10-01-preview' = {
  parent: topicOrderLifecycle
  name: 'inventory-order-settlement'
  properties: {
    maxDeliveryCount: 10
    deadLetteringOnMessageExpiration: true
  }
}

// Grant Azure Service Bus Data Owner Role to Backend Managed Identity
var serviceBusDataOwnerRoleId = '090c5cfd-751d-490a-894a-3ce6f1109419'
resource roleAssignmentServiceBus 'Microsoft.Authorization/roleAssignments@2022-04-01' = {
  name: guid(serviceBusNamespace.id, backendManagedIdentity.id, serviceBusDataOwnerRoleId)
  scope: serviceBusNamespace
  properties: {
    roleDefinitionId: subscriptionResourceId('Microsoft.Authorization/roleDefinitions', serviceBusDataOwnerRoleId)
    principalId: backendManagedIdentity.properties.principalId
    principalType: 'ServicePrincipal'
  }
}

// =============================================================================
// 9. Azure Storage Account (Venue Seat Maps & Official PDF Invoices)
// =============================================================================
resource storageAccount 'Microsoft.Storage/storageAccounts@2023-01-01' = {
  name: storageAccountName
  location: location
  tags: commonTags
  sku: {
    name: environmentName == 'prod' ? 'Standard_ZRS' : 'Standard_LRS'
  }
  kind: 'StorageV2'
  properties: {
    accessTier: 'Hot'
    minimumTlsVersion: 'TLS1_2'
    supportsHttpsTrafficOnly: true
    allowBlobPublicAccess: false
  }
}

resource blobService 'Microsoft.Storage/storageAccounts/blobServices@2023-01-01' = {
  parent: storageAccount
  name: 'default'
}

resource containerVenues 'Microsoft.Storage/storageAccounts/blobServices/containers@2023-01-01' = {
  parent: blobService
  name: 'venues'
  properties: {
    publicAccess: 'None'
  }
}

resource containerInvoices 'Microsoft.Storage/storageAccounts/blobServices/containers@2023-01-01' = {
  parent: blobService
  name: 'hibana-invoices'
  properties: {
    publicAccess: 'None'
  }
}

// Grant Storage Blob Data Contributor Role to Backend Managed Identity
var storageBlobDataContributorRoleId = 'ba92f5b4-2d11-453d-a403-e96b0029c9fe'
resource roleAssignmentStorage 'Microsoft.Authorization/roleAssignments@2022-04-01' = {
  name: guid(storageAccount.id, backendManagedIdentity.id, storageBlobDataContributorRoleId)
  scope: storageAccount
  properties: {
    roleDefinitionId: subscriptionResourceId('Microsoft.Authorization/roleDefinitions', storageBlobDataContributorRoleId)
    principalId: backendManagedIdentity.properties.principalId
    principalType: 'ServicePrincipal'
  }
}

// =============================================================================
// 10. Azure Key Vault with RBAC & Automated Secret Hydration
// =============================================================================
resource keyVault 'Microsoft.KeyVault/vaults@2023-07-01' = {
  name: keyVaultName
  location: location
  tags: union(commonTags, {
    SecurityClassification: 'Confidential'
  })
  properties: {
    tenantId: subscription().tenantId
    sku: {
      family: 'A'
      name: 'standard'
    }
    enableRbacAuthorization: true
    enableSoftDelete: true
    softDeleteRetentionInDays: environmentName == 'prod' ? 90 : 7
    enablePurgeProtection: environmentName == 'prod' ? true : null
    networkAcls: {
      bypass: 'AzureServices'
      defaultAction: 'Allow'
    }
  }
}

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

// Secrets synchronized to AKS via Secrets Store CSI Driver
resource secretWaitingRoom 'Microsoft.KeyVault/vaults/secrets@2023-07-01' = {
  parent: keyVault
  name: 'waiting-room-jwt-secret'
  properties: {
    contentType: 'text/plain'
    value: waitingRoomJwtSecret
  }
}

resource secretGatewayHmac 'Microsoft.KeyVault/vaults/secrets@2023-07-01' = {
  parent: keyVault
  name: 'gateway-hmac-secret'
  properties: {
    contentType: 'text/plain'
    value: gatewayHmacSecret
  }
}

resource secretMercadoPago 'Microsoft.KeyVault/vaults/secrets@2023-07-01' = {
  parent: keyVault
  name: 'mercadopago-webhook-secret'
  properties: {
    contentType: 'text/plain'
    value: mercadoPagoWebhookSecret
  }
}

resource secretDbPassword 'Microsoft.KeyVault/vaults/secrets@2023-07-01' = {
  parent: keyVault
  name: 'postgres-admin-password'
  properties: {
    contentType: 'text/plain'
    value: postgresAdminPassword
  }
}

resource secretRedisPassword 'Microsoft.KeyVault/vaults/secrets@2023-07-01' = {
  parent: keyVault
  name: 'redis-password'
  properties: {
    contentType: 'text/plain'
    value: environmentName == 'prod' ? redisCache!.listKeys().primaryKey : postgresAdminPassword
  }
}

var sbAuthRuleId = resourceId('Microsoft.ServiceBus/namespaces/authorizationRules', serviceBusNamespace.name, 'RootManageSharedAccessKey')
resource secretServiceBusConn 'Microsoft.KeyVault/vaults/secrets@2023-07-01' = {
  parent: keyVault
  name: 'servicebus-connection-string'
  properties: {
    contentType: 'text/plain'
    value: listKeys(sbAuthRuleId, serviceBusNamespace.apiVersion).primaryConnectionString
  }
}

resource secretStorageConn 'Microsoft.KeyVault/vaults/secrets@2023-07-01' = {
  parent: keyVault
  name: 'storage-connection-string'
  properties: {
    contentType: 'text/plain'
    value: 'DefaultEndpointsProtocol=https;AccountName=${storageAccount.name};AccountKey=${storageAccount.listKeys().keys[0].value};EndpointSuffix=${environment().suffixes.storage}'
  }
}

resource secretAppInsights 'Microsoft.KeyVault/vaults/secrets@2023-07-01' = {
  parent: keyVault
  name: 'appinsights-connection-string'
  properties: {
    contentType: 'text/plain'
    value: appInsights.properties.ConnectionString
  }
}

// =============================================================================
// Outputs for Kubernetes Manifests Hydration & CI/CD
// =============================================================================
output aksClusterName string = aksCluster.name
output aksOidcIssuerUrl string = aksCluster.properties.oidcIssuerProfile.issuerURL
output acrLoginServer string = environmentName == 'prod' ? acr!.properties.loginServer : ghcrNamespace
output keyVaultName string = keyVault.name
output keyVaultUri string = keyVault.properties.vaultUri
output managedIdentityClientId string = backendManagedIdentity.properties.clientId
output managedIdentityPrincipalId string = backendManagedIdentity.properties.principalId
output postgresFqdn string = environmentName == 'prod' ? postgresServer!.properties.fullyQualifiedDomainName : 'hibana-postgres.${k8sNamespace}.svc.cluster.local'
output redisHostName string = environmentName == 'prod' ? redisCache!.properties.hostName : 'hibana-redis.${k8sNamespace}.svc.cluster.local'
output serviceBusFqdn string = '${serviceBusNamespace.name}.servicebus.windows.net'
output storageBlobEndpoint string = storageAccount.properties.primaryEndpoints.blob
output appInsightsConnectionString string = appInsights.properties.ConnectionString
