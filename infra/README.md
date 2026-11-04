# Hibana.tk — Cloud Infrastructure & Azure AKS Deployment Guide

This directory contains the complete **Infrastructure as Code (Azure Bicep)**, **Kubernetes (AKS) manifests**, and **CI/CD deployment scripts** required to deploy the Hibana.tk Ticketing Platform to **Microsoft Azure**.

---

## 1. Cloud-Native Architecture Topology

```text
                    Internet (B2C Users & B2B Admins)
                                  │
                        ┌─────────▼─────────┐
                        │  Azure Front Door │
                        │    + Azure WAF    │
                        └─────────┬─────────┘
                                  │
                        ┌─────────▼─────────┐
                        │    AKS Ingress    │
                        │ (NGINX / AppGW)   │
                        └─────────┬─────────┘
         ┌────────────────────────┼────────────────────────┐
         ▼                        ▼                        ▼
┌─────────────────┐     ┌──────────────────┐     ┌─────────────────┐
│ Front-Office UI │     │ Spring Cloud GW  │     │ Back-Office UI  │
│ www.hibana.tk   │     │ api.hibana.tk    │     │ admin.hibana.tk │
└─────────────────┘     └─────────┬────────┘     └─────────────────┘
                                  │ NetworkPolicy (Zero-Trust Perimeter)
                  ┌───────────────┼───────────────┐
                  ▼               ▼               ▼
         ┌──────────────┐┌──────────────┐┌──────────────┐
         │   Catalog    ││  Inventory   ││    Orders    │
         │  (Port 8081) ││  (Port 8082) ││  (Port 8083) │
         └──────┬───────┘└──────┬───────┘└──────┬───────┘
                │               │               │
                └───────────────┼───────────────┘
                                │ Entra Workload Identity + Secrets Store CSI
       ┌───────────────┬────────┴────────┬───────────────┬──────────────┐
       ▼               ▼                 ▼               ▼              ▼
┌──────────────┐┌──────────────┐┌────────────────┐┌─────────────┐┌────────────┐
│ Azure Key    ││ PostgreSQL 17││ Azure Cache    ││ Azure       ││ Azure Blob │
│ Vault (CSI)  ││ + pgvector   ││ for Redis (TLS)││ Service Bus ││ Storage    │
└──────────────┘└──────────────┘└────────────────┘└─────────────┘└────────────┘
```

---

## 2. Directory Inventory

### Azure Bicep (`infra/bicep/`)
- **`main.bicep`**: Complete Azure Infrastructure-as-Code template provisioning:
  1. **Virtual Network (`10.0.0.0/16`)**: Segmented into `snet-ingress` (`10.0.1.0/24`), `snet-aks` (`10.0.2.0/23`), and `snet-data` (`10.0.4.0/24`) with Network Security Groups.
  2. **Observability**: Log Analytics Workspace (`log-hibana-{env}`) and Application Insights (`appi-hibana-{env}`).
  3. **Azure Container Registry (`ACR`)**: Private container registry with `AcrPull` RBAC bound to the AKS Kubelet Managed Identity.
  4. **Azure Kubernetes Service (`AKS`)**: Multi-zone cluster with System + User node pools, OIDC Issuer, **Entra Workload Identity**, **Azure Key Vault Secrets Store CSI Driver** (with auto-rotation), and Calico NetworkPolicies.
  5. **Managed Identity & Federation**: User-Assigned Managed Identity (`id-hibana-backend-{env}`) federated with the `hibana-backend-sa` Kubernetes ServiceAccount.
  6. **Azure Database for PostgreSQL Flexible Server (v17)**: Provisioned with `VECTOR`, `UUID-OSSP`, and `PG_TRGM` extensions and the `ticketing_db` database.
  7. **Azure Cache for Redis**: Enforced TLS 1.2 on port `6380` for Redlock distributed seat locks and Virtual Waiting Room sorted sets (`ZSET`).
  8. **Azure Service Bus**: Namespace with topics `event.lifecycle.v1` (`ticketing-seat-generator`), `ticket.reservation.v1` (`orders-reservation-tracker`), and `order.lifecycle.v1` (`inventory-order-settlement`).
  9. **Azure Blob Storage**: Containers `venues` (seatmap JSON layouts) and `hibana-invoices` (official PDF invoices).
  10. **Azure Key Vault**: RBAC-governed vault automatically hydrated with database, Redis, Service Bus, Storage, and Application Insights secrets.
- **`main.bicepparam`**: Parameter file reading sensitive values from environment variables.

### Kubernetes Manifests (`infra/k8s/`)
- **`kustomization.yaml`**: Declarative Kustomize entrypoint bundling the entire AKS stack.
- **`workload-identity.yaml`**: Namespace `hibana-prod` and `ServiceAccount` (`hibana-backend-sa`) annotated for Azure Workload Identity.
- **`secret-provider-class.yaml`**: `SecretProviderClass` mounting secrets from Azure Key Vault via CSI and syncing to Kubernetes Secret `hibana-secrets`.
- **`configmap.yaml`**: Centralized `ConfigMap` (`hibana-config`) configuring CoreDNS URLs, SSL flags, Entra ID OIDC endpoints, Service Bus, and Blob Storage.
- **`gateway-deployment.yaml`**: Deployment, Service, and PodDisruptionBudget for `ticketing-app-gateway` (`:8080`).
- **`catalog-deployment.yaml`**: Deployment, Service, and PodDisruptionBudget for `ticketing-app-catalog` (`:8081`).
- **`inventory-deployment.yaml`**: High-concurrency Deployment, Service, and PodDisruptionBudget for `ticketing-app-inventory` (`:8082`).
- **`orders-deployment.yaml`**: Deployment, Service, and PodDisruptionBudget for `ticketing-app-orders` (`:8083`).
- **`frontend-deployment.yaml`**: Multi-stage Nginx Deployments and Services for B2C (`ticketing-frontend`) and B2B (`ticketing-admin`).
- **`hpa-autoscaling.yaml`**: `HorizontalPodAutoscaler` (`autoscaling/v2`) rules scaling inventory up to 15 replicas during high-demand ticket drops.
- **`network-policy.yaml`**: Zero-Trust ingress isolation (blocking direct pod access and allowing only Gateway -> Backend and Orders -> Inventory Saga confirmation).
- **`ingress.yaml`**: TLS-terminated NGINX Ingress routing `api.hibana.tk`, `www.hibana.tk`, and `admin.hibana.tk` with WebSocket & SSE streaming support.

---

## 3. One-Command Deployment to Azure AKS

### Option A: PowerShell (Windows)
```powershell
.\infra\deploy-azure.ps1 `
  -ResourceGroupName "rg-hibana-prod" `
  -Location "eastus" `
  -EnvironmentName "prod" `
  -PostgresPassword "<STRONG_DB_PASSWORD>" `
  -WaitingRoomJwtSecret "<256BIT_WAITING_ROOM_SECRET>" `
  -GatewayHmacSecret "<256BIT_GATEWAY_HMAC_SECRET>"
```

### Option B: Bash (Linux / macOS / CI)
```bash
export HIBANA_POSTGRES_PASSWORD="<STRONG_DB_PASSWORD>"
export HIBANA_WAITING_ROOM_JWT_SECRET="<256BIT_WAITING_ROOM_SECRET>"
export HIBANA_GATEWAY_HMAC_SECRET="<256BIT_GATEWAY_HMAC_SECRET>"

./infra/deploy-azure.sh
```

### Option C: GitHub Actions CI/CD
The repository includes [`.github/workflows/deploy-aks.yml`](../.github/workflows/deploy-aks.yml), which authenticates to Azure via OIDC (`azure/login@v2`) and executes the full Bicep + ACR + AKS deployment pipeline.

---

## 4. Local Development vs. Azure Production Matrix

| Component | Local (`docker-compose.yml`) | Production (`infra/` + Azure AKS) |
| :--- | :--- | :--- |
| **Container Orchestration** | Docker Compose (`localhost`) | Azure Kubernetes Service (AKS) + Kustomize |
| **Frontend Containers** | `Dockerfile` (Vite Dev Server) | `Dockerfile.prod` (Multi-stage Bun build + Nginx unprivileged `:8080`) |
| **Database** | `pgvector/pgvector:pg17` (`:5432`, `sslmode=disable`) | Azure Database for PostgreSQL Flexible Server (`sslmode=require`) |
| **Cache & Locks** | `redis:7-alpine` (`:6379`, `REDIS_SSL=false`) | Azure Cache for Redis (`:6380`, `REDIS_SSL=true`) |
| **Event Bus** | Floci-AZ AMQP (`:5673`) | Azure Service Bus Namespace (Topics & Subscriptions) |
| **Object Storage** | Floci-AZ Blob (`:4577/devstoreaccount1`) | Azure Storage Account (`venues` & `hibana-invoices` containers) |
| **Secrets & Key Vault** | Floci-AZ Key Vault (`:4577`) | Azure Key Vault + Secrets Store CSI Driver + Workload Identity |
| **Identity (IAM)** | Floci-AZ Entra ID Emulator (`entra-seed.json`) | Microsoft Entra ID (`login.microsoftonline.com`) |
