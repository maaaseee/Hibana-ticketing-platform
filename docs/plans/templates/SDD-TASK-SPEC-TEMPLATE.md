# Spec-Driven Development (SDD) Task Specification Template

> **Task ID & Name:** `[MODULE]-[FEATURE]-[TASK_NUM]` (e.g., `CATALOG-WRITE-01`)  
> **Bounded Context:** `[IAM | Catalog | Ticketing | Orders | Support | Recommendation]`  
> **Target Git Worktree:** `.worktrees/[role]-[feature-slug]`  
> **Assigned Agent:** `[Planner | Infra-Data | Backend | Frontend | QA-Integrator]`  

---

## 1. Context & Business Intent
*Brief description of what this specification accomplishes and which domain events or user stories it implements.*

---

## 2. Locked Dependencies Specification
> **CRITICAL RULE:** No agent may introduce dependencies not listed below. Any unbudgeted library requires an official spec amendment from the Planner.

### 2.1 Backend (`build.gradle`)
```groovy
// List exact coordinates with versions:
// e.g., implementation 'tools.jackson.datatype:jackson-datatype-jsr310:2.18.2'
```

### 2.2 Frontend (`package.json`)
```json
// List exact npm packages:
// e.g., "lucide-vue-next": "^0.475.0"
```

### 2.3 Infrastructure (`docker-compose.yml`)
```yaml
// List exact container images and ports
```

---

## 3. Explicit MUST DO (Requirements & Invariants)
- [ ] **Functional Requirement 1:** ...
- [ ] **Domain Invariant 1:** Aggregate root must reject ...
- [ ] **Outbox Requirement:** Persist domain event `[EventName]` into `outbox_events` within the local ACID transaction.
- [ ] **Contract Delivery:** Expose REST DTOs / WebSocket contracts matching OpenAPI 3.1.
- [ ] **Isolation:** Commit all code strictly inside `.worktrees/[role]-[feature-slug]`.

---

## 4. Explicit MUST NOT DO (Strict Anti-Patterns & Boundaries)
- ❌ **MUST NOT** import Spring Framework or Jakarta Persistence into `domain/` (pure Java only).
- ❌ **MUST NOT** perform cross-service direct SQL queries or JPA `@ManyToOne` joins across bounded contexts.
- ❌ **MUST NOT** install, bump, or inject unapproved dependencies into build configuration files.
- ❌ **MUST NOT** render seat maps in the Back-Office using SVG or individual DOM elements (HTML5 Canvas is mandatory).
- ❌ **MUST NOT** mutate state without emitting an event to `outbox_events`.
- ❌ **MUST NOT** modify files outside the assigned bounded context directory.

---

## 5. Verification & Acceptance Gate (Executable Checkpoints)

```powershell
# C1: Clean build and compilation
.\gradlew.bat clean compileJava # or npm run build

# C2: Unit and domain tests
.\gradlew.bat test

# C3: Integration and health verification
curl http://localhost:8080/actuator/health
```
