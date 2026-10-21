# Architecture Diagram 01: System Containers & Topology (C4 Level 2)

```mermaid
flowchart TB
    subgraph CLIENT_TIER["Client Tier"]
        B2C["B2C Web Client (apps/b2c-web)<br/>Vue 3 + Vite + Pinia<br/>Port: 3000"]
        B2B["B2B Admin Portal (apps/b2b-admin)<br/>Vue 3 + HTML5 Canvas<br/>Port: 3001"]
    end

    subgraph EDGE_TIER["Edge & Gateway Tier"]
        GW["Spring Cloud Gateway<br/>Token Bucket Rate Limiter + CheckoutTokenFilter<br/>Port: 8080"]
    end

    subgraph SERVICES_TIER["Backend Microservices Tier (AKS Cluster)"]
        CAT["Catalog Service<br/>Spring Boot 4 / Java 25 (CQRS & Mediator)<br/>Port: 8081"]
        TICK["Ticketing Service<br/>Spring Boot 4 / Java 25 (Redlock Engine)<br/>Port: 8082"]
        ORD["Order Service<br/>Spring Boot 4 / Java 25 (Double-Entry Ledger)<br/>Port: 8083"]
    end

    subgraph STORAGE_TIER["Isolated Persistence & Cache Tier"]
        subgraph DB_CATALOG["Catalog Database"]
            PG_CAT[("PostgreSQL 16/17 (catalog_db)<br/>pgvector HNSW Index")]
        end
        subgraph DB_TICKETING["Ticketing Database"]
            PG_TICK[("PostgreSQL 16/17 (ticketing_db)<br/>ACID Inventory & Outbox")]
        end
        subgraph DB_ORDER["Order Database"]
            PG_ORD[("PostgreSQL 16/17 (order_db)<br/>Financial Ledger & Saga")]
        end
        subgraph CACHE_TIER["In-Memory Cache & Concurrency"]
            REDIS[("Azure Managed Redis Cluster<br/>Sorted Sets (Waiting Room) & Redlock")]
        end
    end

    subgraph MESSAGING_TIER["Asynchronous Messaging Backbone"]
        SB[["Azure Service Bus<br/>Topics & Subscriptions (AMQP 1.0)"]]
    end

    %% Protocols and Interactions
    B2C -->|"HTTPS / REST (TLS 1.3)"| GW
    B2B -->|"HTTPS / REST (TLS 1.3)"| GW

    GW -->|"HTTPS / REST (v1 API)"| CAT
    GW -->|"HTTPS / REST (Checkout Gated)"| TICK
    GW -->|"HTTPS / REST (Order API)"| ORD
    GW -.->|"TCP / RESP (Rate Limiting)"| REDIS

    CAT -->|"TCP / JDBC (PostgreSQL Wire)"| PG_CAT
    TICK -->|"TCP / JDBC (PostgreSQL Wire)"| PG_TICK
    ORD -->|"TCP / JDBC (PostgreSQL Wire)"| PG_ORD

    TICK -->|"TCP / RESP (Redlock 10m TTL)"| REDIS
    TICK -->|"TCP / RESP (ZSET Waiting Room)"| REDIS

    CAT -->|"AMQP / Azure SDK (Outbox Relay)"| SB
    TICK -->|"AMQP / Azure SDK (Outbox Relay)"| SB
    ORD -->|"AMQP / Azure SDK (Outbox Relay)"| SB

    SB -.->|"AMQP (Saga Choreography)"| TICK
    SB -.->|"AMQP (Saga Choreography)"| ORD
```

---

## Container Architecture Specifications

| Container | Core Responsibility | Tech Stack | Primary Datastore / Protocol |
| :--- | :--- | :--- | :--- |
| **B2C Web Client** | Public customer experience, event discovery, queue waiting, interactive seat picker, checkout. | Vue 3, Vite, Pinia, TailwindCSS | HTTPS / REST to Gateway |
| **B2B Admin Portal** | Event organizers & operations portal, venue creation, seat map editor with high-performance canvas rendering. | Vue 3, Vite, HTML5 Canvas, Pinia | HTTPS / REST to Gateway (Isolated VNet) |
| **API Gateway** | Perimeter entry, TLS termination, IP-based Token Bucket rate limiting (100 req/min), `X-Checkout-Token` verification, trace propagation. | Spring Boot 4, Spring Cloud Gateway, Redis Reactive | Redis (Token Bucket & Session Claims) |
| **Catalog Service** | Events and venue metadata management, schedule indexing, semantic search queries using vector similarity. | Java 25, Spring Boot 4, Spring Data JPA, pgvector | PostgreSQL (`catalog_db` with `pgvector` HNSW index) |
| **Ticketing Service** | High-concurrency seat inventory, atomic reservation holds via Redlock, bulk seat provisioning, virtual waiting room queue. | Java 25, Spring Boot 4, Redisson / Lettuce, Outbox Pattern | PostgreSQL (`ticketing_db`) + Redis (Redlock & ZSETs) |
| **Order Service** | Checkout orchestration, payment lifecycle integration, double-entry ledger bookkeeping (`com.aieta.pocket`), Choreographed Saga participant. | Java 25, Spring Boot 4, Transactional Outbox | PostgreSQL (`order_db`) |
| **Azure Service Bus** | Resilient asynchronous event-driven communications backbone connecting microservices with at-least-once delivery. | Azure Service Bus (Topics & Subscriptions) | AMQP 1.0 protocol |
| **Azure Cache for Redis** | Microsecond-latency cache, atomic distributed locks with TTL, waiting room sorted set queues, and WebSocket Pub/Sub. | Redis 7+ Cluster (Premium Tier) | TCP / RESP protocol |