# Hibana.tk — Documentation Index

Welcome to the technical and architectural documentation repository for the **Hibana.tk** platform.

---

## Directory Taxonomy

```text
docs/
├── architecture/                   # High-level architecture, C4 views, and system design
│   ├── Ticketing-app.md            # Comprehensive technical, operational & AI specification
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
│   ├── frontend-catalog-browse-and-filters-sdd.md
│   ├── catalog-service-plan.md
│   └── catalog-service-plan-2nd.md
└── history/                        # Historical session logs and architecture decision records
    └── 2026-10-15-catalog-refactor-session.md
```

---

## Core References

1. **System Architecture Specification:** [Ticketing-app.md](file:///C:/Users/maxe1/Documents/Repos/Spring%20projects%20on%20Github/Ticketing%20app/docs/architecture/Ticketing-app.md)
2. **API & Event Documentation Guide:** [api-documentation-guide.md](file:///C:/Users/maxe1/Documents/Repos/Spring%20projects%20on%20Github/Ticketing%20app/docs/api-documentation-guide.md)
3. **OpenAPI 3.1 Gateway Contract:** [gateway-api.yaml](file:///C:/Users/maxe1/Documents/Repos/Spring%20projects%20on%20Github/Ticketing%20app/docs/specs/openapi/gateway-api.yaml)
4. **AsyncAPI 3.0 Event Bus Contract:** [servicebus-events.yaml](file:///C:/Users/maxe1/Documents/Repos/Spring%20projects%20on%20Github/Ticketing%20app/docs/specs/asyncapi/servicebus-events.yaml)
5. **Architecture Diagrams:** [docs/architecture/diagrams/](file:///C:/Users/maxe1/Documents/Repos/Spring%20projects%20on%20Github/Ticketing%20app/docs/architecture/diagrams/)
6. **Spec-Driven Development Template:** [SDD-TASK-SPEC-TEMPLATE.md](file:///C:/Users/maxe1/Documents/Repos/Spring%20projects%20on%20Github/Ticketing%20app/docs/plans/templates/SDD-TASK-SPEC-TEMPLATE.md)
7. **Active Implementation Plan:** [frontend-catalog-browse-and-filters-sdd.md](file:///C:/Users/maxe1/Documents/Repos/Spring%20projects%20on%20Github/Ticketing%20app/docs/plans/frontend-catalog-browse-and-filters-sdd.md)
