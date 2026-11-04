# Hibana.tk — Documentation Index

Welcome to the technical and architectural documentation repository for the **Hibana.tk** platform.

---

## Directory Taxonomy

```text
docs/
├── architecture/                   # High-level architecture, C4 views, and system design
│   ├── Ticketing-app.md            # Comprehensive technical, operational & AI specification
│   ├── azure-authentication-floci-emulation.md # Azure Auth & Entra ID Emulation Guide
│   └── diagrams/                   # Architectural Mermaid diagrams & sequence flows
│       ├── 01-system-containers-c4.md
│       ├── 02-seat-hold-reservation-lifecycle.md
│       ├── 03-distributed-checkout-saga.md
│       ├── 04-messaging-event-topology.md
│       ├── 05-virtual-waiting-room.md
│       ├── 06-data-storage-boundaries.md
│       ├── 07-frontend-checkout-state-machine.md
│       └── 08-azure-cloud-infrastructure.md
├── specs/                          # Product proposals, API contracts & functional feature specifications
│   ├── openapi/                    # OpenAPI 3.1 contracts (Gateway aggregated endpoints)
│   │   └── gateway-api.yaml
│   ├── asyncapi/                   # AsyncAPI 3.0 contracts (Service Bus topics, Outbox & WebSockets)
│   │   └── servicebus-events.yaml
│   ├── future-pages-proposal.md    # Frontend views, routes, and layout design
│   └── qr-access-control.md        # Dynamic QR, TOTP offline validation & access gates
├── api-documentation-guide.md      # Complete Microservices API & Event Mesh documentation guide
├── plans/                          # Execution roadmaps and Spec-Driven Development (SDD) plans
│   ├── templates/                  # Reusable engineering templates
│   │   ├── SDD-TASK-SPEC-TEMPLATE.md
│   │   ├── MICROSERVICE-PLAN-TEMPLATE.md
│   │   └── PLAN-TEMPLATE.md
│   ├── admin-portal-implementation-plan.md # B2B Admin & Ops Portal Implementation Plan
│   ├── frontend-catalog-browse-and-filters-sdd.md
│   ├── catalog-service-plan.md
│   └── catalog-service-plan-2nd.md
└── history/                        # Historical session logs and architecture decision records
    └── 2026-10-15-catalog-refactor-session.md
```

---

## Core References

1. **System Architecture Specification:** [Ticketing-app.md](./architecture/Ticketing-app.md)
2. **Azure Auth & Entra ID Emulation Guide:** [azure-authentication-floci-emulation.md](./architecture/azure-authentication-floci-emulation.md)
3. **B2B Admin Portal Implementation Plan:** [admin-portal-implementation-plan.md](./plans/admin-portal-implementation-plan.md)
4. **API & Event Documentation Guide:** [api-documentation-guide.md](./api-documentation-guide.md)
5. **OpenAPI 3.1 Gateway Contract:** [gateway-api.yaml](./specs/openapi/gateway-api.yaml)
6. **AsyncAPI 3.0 Event Bus Contract:** [servicebus-events.yaml](./specs/asyncapi/servicebus-events.yaml)
7. **Architecture Diagrams:** [docs/architecture/diagrams/](./architecture/diagrams/)
8. **Spec-Driven Development Template:** [SDD-TASK-SPEC-TEMPLATE.md](./plans/templates/SDD-TASK-SPEC-TEMPLATE.md)
9. **Active Implementation Plan:** [frontend-catalog-browse-and-filters-sdd.md](./plans/frontend-catalog-browse-and-filters-sdd.md)
