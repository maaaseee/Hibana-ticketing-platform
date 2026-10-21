## graphify

This project has a knowledge graph at graphify-out/ with god nodes, community structure, and cross-file relationships.

Rules:
- For codebase questions, first run `graphify query "<question>"` when graphify-out/graph.json exists. Use `graphify path "<A>" "<B>"` for relationships and `graphify explain "<concept>"` for focused concepts. These return a scoped subgraph, usually much smaller than GRAPH_REPORT.md or raw grep output.
- If graphify-out/wiki/index.md exists, use it for broad navigation instead of raw source browsing.
- Read graphify-out/GRAPH_REPORT.md only for broad architecture review or when query/path/explain do not surface enough context.
- After modifying code, run `graphify update .` to keep the graph current (AST-only, no API cost).
- A committed graphify-out/graph.json is rewritten whole on every build, so a large diff to it in a pull request is expected. Do not flag it as churn or suggest skipping the graph rebuild or its hooks.

---

## Technical Stack & Architecture Standards

### Core Technology Versions
- **Backend**: **Java 25** and **Spring Boot 4**. Leverage modern Java features (records, pattern matching, virtual threads) and Spring Boot 4 conventions.
- **Frontend**: **Vue.js (Latest / Vue 3)** with Composition API (`<script setup>`), Pinia for state management, and Vite.
- **Local Emulation**: Use **Floci-AZ** for zero-cost local emulation of Azure components (PostgreSQL, Cosmos DB, Service Bus, Redis, Key Vault) and Ollama/LocalAI for embeddings.

### Frontend Application Separation
- **Front-Office (B2C)**: High-concurrency public client (`www.amaryllis-tickets.com`). Optimized for speed, CDN caching, route lazy loading, and Virtual Waiting Room integration.
- **Back-Office (B2B)**: Operations/Admin portal (`admin.amaryllis-tickets.com`). Must be physically and logically isolated from B2C traffic. Must use **HTML5 Canvas** for high-performance interactive seat map rendering (avoid bloated DOM/SVG trees).

### Persistence & Data Access
- **Azure Database for PostgreSQL (Flexible Server)**: Primary relational store for transactional domains (Ticketing, Financials, Orders). Uses `pgvector` with HNSW indexing for AI embeddings.
- **Azure Cosmos DB for NoSQL**: Low-latency, flexible document store for Event & Venue Catalog metadata.
- **Transactional Outbox Pattern**: Every cross-service state mutation must write the business entity and domain event to `outbox_events` in the same local ACID transaction. An asynchronous CDC or worker relays events to Azure Service Bus with at-least-once delivery.

### Concurrency, Caching & Traffic Management
- **Distributed Locking**: Atomic seat reservation using Azure Cache for Redis via the **Redlock** algorithm. Key pattern: `lock:event:{eventId}:seat:{seatId}` with a strict 10-minute TTL. Releases occur passively via TTL or actively via Saga compensation.
- **Virtual Waiting Room**: High-demand queueing using Redis Sorted Sets (`queue:event:{eventId}`) with millisecond timestamps as score.
- **Gateway Checkout Filter**: Spring Cloud Gateway must enforce `CheckoutTokenFilterFactory` on `/api/tickets/reserve/**`, validating `X-Checkout-Token` before passing traffic to the Ticketing Service.
- **Rate Limiting**: Public endpoints limited to 100 req/min per IP using Redis Token Bucket at the Gateway.

### Security, IAM & Observability
- **Microsoft Entra ID & Managed Identities**: Passwordless inter-service communication. Never commit credentials or plain-text connection strings.
- **RBAC Enforcement**: Strict role gating on all endpoints: `ROLE_USER`, `ROLE_ADMIN` (venue/event lifecycle), and `ROLE_OPERATOR` (disputes, read orders, support chat).
- **Secrets Management**: Centralized in Azure Key Vault via Secrets Store CSI Driver.
- **Observability**: Propagate `Trace-ID` through Spring Cloud Gateway across all HTTP requests and Service Bus events. Integrate with Application Insights and Azure Monitor.

---

## Domain-Driven Design (DDD) & Service Boundaries

### Strict Bounded Context Rules
- **Microservice Autonomy**: Every microservice owns its datastore exclusively. Cross-context direct database joins, shared JPA entities, or cross-database transactions are strictly forbidden.
- **Communication Protocols**:
  - **Queries (Synchronous)**: Spring Cloud OpenFeign over internal CoreDNS, wrapped with **Resilience4j** Circuit Breakers and fallbacks.
  - **Commands / Mutations (Asynchronous)**: Azure Service Bus Topics and Subscriptions with choreographed **Saga** pattern and Transactional Outbox.

### Bounded Contexts & Domain Events
1. **Identity & Access Management (IAM)**
   - Responsibility: Entra ID integration, RBAC, Managed Identities, token issuance.
   - Published Events: `UserRegistered`, `UserBanned`, `RoleUpdated`.
2. **Event & Venue Catalog**
   - Responsibility: Recintos, espectáculos, fechas, sectores, capacidades (Cosmos DB NoSQL).
   - Published Events: `EventCreated`, `EventCancelled`, `VenueCapacityChanged`.
3. **Ticketing & Inventory**
   - Responsibility: Seat lifecycle, distributed locking (Redlock), bulk seat generation, final issuance.
   - Published Events: `SeatLocked`, `TicketIssued`, `TicketRefunded`.
4. **Order & Financial Engine**
   - Responsibility: Shopping carts, payment orchestration, virtual wallets, double-entry bookkeeping (`com.aieta.pocket`).
   - Published Events: `OrderCreated`, `PaymentApproved`, `PaymentFailed`.
5. **Customer Support & Ops**
   - Responsibility: Support tickets, dispute management, live chat over Spring WebFlux WebSockets with Redis Pub/Sub (`chat.room.{ticketId}`), RAG-assisted answers.
   - Published Events: `ChatSessionStarted`, `TicketDisputed`.
6. **Recommendation Engine (AI)**
   - Responsibility: Vector embeddings, cosine similarity queries (`1 - (embedding <=> :userVector)`), MLflow experiment tracking.
   - Published Events: `UserRecommendationsReady`.

