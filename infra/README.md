# Hibana.tk - Cloud Infrastructure & DevSecOps Architecture

This directory contains the production-ready infrastructure as code (IaC) and Kubernetes deployment manifests for the Hibana.tk Ticketing Platform on **Microsoft Azure**.

---

## 1. Overview & Architecture

```
                    Internet
                       │
             ┌─────────▼─────────┐
             │  Azure Front Door │
             │    + Azure WAF    │
             └─────────┬─────────┘
                       │
             ┌─────────▼─────────┐
             │    AKS Ingress    │
             │ Spring Cloud GW   │  (Port 8080)
             └─────────┬─────────┘
                       │ NetworkPolicy (Only Gateway allowed)
       ┌───────────────┼───────────────┐
       ▼               ▼               ▼
┌──────────────┐┌──────────────┐┌──────────────┐
│   Catalog    ││  Inventory   ││    Orders    │
│  (Port 8081) ││  (Port 8082) ││  (Port 8083) │
└──────┬───────┘└──────┬───────┘└──────┬───────┘
       │               │               │
       └───────────────┼───────────────┘
                       │ Entra Workload Identity (Passwordless)
       ┌───────────────┼──────────────────────────────┐
       ▼               ▼                              ▼
┌──────────────┐┌──────────────────────────────┐┌──────────────┐
│ Azure Key    ││ Azure Database for PostgreSQL││ Azure Cache  │
│ Vault (CSI)  ││ Flexible Server (pgvector)   ││ for Redis    │
└──────────────┘└──────────────────────────────┘└──────────────┘
```

---

## 2. Directory Structure

- `infra/bicep/main.bicep`: Azure Bicep template provisioning:
  - Azure Key Vault with Azure RBAC authorization enabled.
  - User-Assigned Managed Identity (`id-hibana-backend-{env}`).
  - Federated Identity Credential linking AKS OIDC Issuer with Entra ID.
  - Key Vault Secrets User role assignment (`4633458b-17de-408a-b874-0445c86b69e6`).
  - Secret definitions for `waiting-room-jwt-secret`, `gateway-hmac-secret`, `mercadopago-webhook-secret`, `postgres-admin-password`, and `redis-password`.
- `infra/k8s/workload-identity.yaml`: Kubernetes `ServiceAccount` annotated with Azure Workload Identity client ID.
- `infra/k8s/secret-provider-class.yaml`: Secrets Store CSI Driver `SecretProviderClass` mounting secrets from Azure Key Vault and synchronizing them into native Kubernetes `Secret` resources (`hibana-secrets`).
- `infra/k8s/network-policy.yaml`: Kubernetes NetworkPolicies enforcing perimeter isolation (blocking direct access to microservices and permitting ingress strictly from Gateway pods and authorized Feign inter-service channels).

---

## 3. Deployment Guide

### Step 1: Deploy Azure Key Vault & Workload Identity via Bicep
```bash
az deployment group create \
  --resource-group rg-hibana-prod \
  --template-file infra/bicep/main.bicep \
  --parameters \
    environmentName=prod \
    aksOidcIssuerUrl="$(az aks show -g rg-hibana-prod -n aks-hibana-prod --query "oidcIssuerProfile.issuerUrl" -o tsv)"
```

### Step 2: Configure Secrets in Key Vault
```bash
# Populate production secrets in Key Vault (never commit to git)
az keyvault secret set --vault-name kv-hibana-prod --name waiting-room-jwt-secret --value "<PROD_256BIT_SECRET>"
az keyvault secret set --vault-name kv-hibana-prod --name gateway-hmac-secret --value "<PROD_256BIT_SECRET>"
az keyvault secret set --vault-name kv-hibana-prod --name mercadopago-webhook-secret --value "<MERCADOPAGO_SECRET>"
az keyvault secret set --vault-name kv-hibana-prod --name postgres-admin-password --value "<DB_PASSWORD>"
az keyvault secret set --vault-name kv-hibana-prod --name redis-password --value "<REDIS_PASSWORD>"
```

### Step 3: Apply Kubernetes Manifests to AKS
```bash
# Apply Workload Identity ServiceAccount
kubectl apply -f infra/k8s/workload-identity.yaml

# Apply Secrets Store CSI Driver Provider Class
kubectl apply -f infra/k8s/secret-provider-class.yaml

# Apply Ingress Isolation Network Policies
kubectl apply -f infra/k8s/network-policy.yaml
```

---

## 4. Local Development vs. Production Mapping

| Resource | Local Development (`docker-compose.yml`) | Production (`infra/` + Azure) |
|---|---|---|
| Key Vault | Floci-AZ (`http://localhost:4577/devstoreaccount1-keyvault/`) | Azure Key Vault + Secrets Store CSI Driver |
| Identity & RBAC | Floci-AZ Entra Emulator (`entra-seed.json`) | Microsoft Entra ID + App Registrations (App Roles) |
| Database | PostgreSQL 17 + pgvector (localhost:5432) | Azure Database for PostgreSQL Flexible Server + Managed Identity |
| Distributed Cache | Redis 7 Alpine (`--requirepass`) | Azure Cache for Redis (Premium with Private Endpoint) |
| Network Isolation | Localhost binding `127.0.0.1` | AKS NetworkPolicies + Private Endpoints + Front Door WAF |
