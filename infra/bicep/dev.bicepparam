using './main.bicep'

param environmentName = 'dev'
param k8sNamespace = 'hibana-dev'
param k8sServiceAccountName = 'hibana-backend-sa'
param postgresAdminLogin = 'ticketing_admin'
// 1x Standard_D4s_v6 (4 vCPUs, 16 GB RAM) fits within the 4-vCPU Free Trial quota
// in eastus2 and supports 20+ pods during k6 load testing.
param aksNodeVmSize = 'Standard_D4s_v6'

// Provide secrets securely via environment variables during deployment
param postgresAdminPassword = readEnvironmentVariable('HIBANA_POSTGRES_PASSWORD', 'ChangeMe_StrongPsqlPass_2026!')
param waitingRoomJwtSecret = readEnvironmentVariable('HIBANA_WAITING_ROOM_JWT_SECRET', 'ChangeMe_WaitingRoomJwtSecret_256Bit_2026!')
param gatewayHmacSecret = readEnvironmentVariable('HIBANA_GATEWAY_HMAC_SECRET', 'ChangeMe_GatewayInternalHmacSecret_256Bit_2026!')
param mercadoPagoWebhookSecret = readEnvironmentVariable('HIBANA_MP_WEBHOOK_SECRET', 'ChangeMe_MercadoPagoWebhookSecret_2026!')
