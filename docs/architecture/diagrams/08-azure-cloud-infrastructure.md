# Architecture Diagram 08: Production Azure Cloud Infrastructure & Network Topology

```mermaid
flowchart TB
    subgraph PERIMETER["Perimeter & Edge Tier (Public Internet)"]
        INTERNET(("Public Internet Clients<br/>(B2C Users & B2B Admins)"))
        AFD["Azure Front Door (Global Anycast CDN & WAF Policy)<br/>Custom Domains: www.amaryllis-tickets.com & admin.amaryllis-tickets.com"]
        BLOB_STATIC[("Azure Blob Storage<br/>(Static Web Assets: Vue 3 B2C/B2B Apps)")]
    end

    subgraph AZURE_VNET["Azure Virtual Network (VNet: 10.0.0.0/16)"]
        subgraph SUBNET_INGRESS["Subnet: Ingress & Gateway (10.0.1.0/24)"]
            APP_GW["Azure Application Gateway v2 / AKS Ingress Controller<br/>(Private IP, SSL Offloading, WAF Mode)"]
            SCG["Spring Cloud Gateway Pods<br/>(Token Bucket Rate Limiter & Token Filters)"]
        end

        subgraph AKS_CLUSTER["Azure Kubernetes Service (AKS Cluster: 10.0.2.0/23)"]
            subgraph NS_SERVICES["Namespace: 'services' (Auto-scaled by KEDA & HPA)"]
                POD_CAT["Catalog Service Pods<br/>(Java 25 / Spring Boot 4)"]
                POD_TICK["Ticketing Service Pods<br/>(Redlock & Seat Reservation Engine)"]
                POD_ORD["Order Service Pods<br/>(Financial Ledger & Saga Orchestration)"]
            end

            subgraph NS_MONITORING["Namespace: 'monitoring'"]
                OTEL["OpenTelemetry Collector DaemonSet<br/>(Trace-ID Correlation Engine)"]
            end
        end

        subgraph SUBNET_DATA["Subnet: Private Endpoints & Data Stores (10.0.4.0/24)"]
            PEP_PG["Private Endpoint: PostgreSQL"]
            PEP_REDIS["Private Endpoint: Redis"]
            PEP_SB["Private Endpoint: Service Bus"]
            PEP_KV["Private Endpoint: Key Vault"]
        end
    end

    subgraph AZURE_PAAS["Azure Managed PaaS Data Services"]
        PG_FLEX[("Azure Database for PostgreSQL (Flexible Server)<br/>- catalog_db (pgvector)<br/>- ticketing_db<br/>- order_db")]
        REDIS_PREM[("Azure Cache for Redis (Premium Tier with Clustering)<br/>- Virtual Waiting Room ZSETs<br/>- Redlock Concurrency Keys")]
        SB_PREM[["Azure Service Bus (Premium Namespace)<br/>- Topics & Subscriptions<br/>- Partitioned AMQP 1.0"]]
        AKV["Azure Key Vault<br/>(Secrets, Ed25519 Keys, Managed Identities via CSI Driver)"]
    end

    subgraph OBSERVABILITY["Azure Observability & Governance"]
        APP_INSIGHTS["Application Insights & Azure Monitor<br/>(Distributed Tracing, APM & Alerting)"]
        LOG_ANALYTICS[("Log Analytics Workspace (KQL Query Engine)")]
    end

    %% Network Connections
    INTERNET -->|"HTTPS / TLS 1.3"| AFD
    AFD -->|"Origin: Static CDN"| BLOB_STATIC
    AFD -->|"Origin: Dynamic API (Secure Origin Routing)"| APP_GW

    APP_GW --> SCG
    SCG --> POD_CAT
    SCG --> POD_TICK
    SCG --> POD_ORD

    POD_CAT --> PEP_PG --> PG_FLEX
    POD_TICK --> PEP_PG
    POD_ORD --> PEP_PG

    POD_TICK --> PEP_REDIS --> REDIS_PREM
    SCG -.-> PEP_REDIS

    POD_CAT --> PEP_SB --> SB_PREM
    POD_TICK --> PEP_SB
    POD_ORD --> PEP_SB

    AKS_CLUSTER -.->|"Secrets Store CSI Driver"| PEP_KV --> AKV

    %% Telemetry
    OTEL --> APP_INSIGHTS
    APP_INSIGHTS --> LOG_ANALYTICS
    POD_CAT -.-> OTEL
    POD_TICK -.-> OTEL
    POD_ORD -.-> OTEL
```

---

## Local Emulation vs. Azure Production Mapping Matrix

| Local Architecture Component (`docker-compose.yml` / Dev) | Production Azure Cloud Architecture | Scaling & SLA Mechanism |
| :--- | :--- | :--- |
| **`ticketing-postgres` (PostgreSQL 17 Container)** | **Azure Database for PostgreSQL (Flexible Server)** | Zone-redundant High Availability (HA), compute autoscaling, automated point-in-time recovery (PITR). |
| **`ticketing-redis` (Redis 7 Alpine Container)** | **Azure Cache for Redis (Premium P1 Cluster)** | Multi-AZ clustering, Redis data persistence (RDB/AOF), 99.99% availability SLA. |
| **`floci-az` (Local Azure Service Bus Emulator)** | **Azure Service Bus (Premium Tier)** | Dedicated messaging units (MUs), cross-region disaster recovery (Geo-DR), partitioned topics. |
| **Local Environment Variables (`.env`)** | **Azure Key Vault + Managed Identities** | Passwordless authentication (Azure AD/Entra ID), CSI Secrets Store driver injecting k8s secrets. |
| **Docker Compose Bridge Network** | **Azure Virtual Network (VNet) + Private Endpoints** | Microservices isolated from public internet; zero public IPs on backend databases. |
| **Spring Boot Local Logging (SLF4J Console)** | **Application Insights + Log Analytics** | W3C `Trace-ID` distributed end-to-end correlation across all HTTP and AMQP spans. |
| **Local Vite Dev Server (Ports 3000 / 3001)** | **Azure Front Door + Azure Blob Static Website** | Global Anycast Edge CDN, SSL termination at POPs, DDoS Protection Standard. |