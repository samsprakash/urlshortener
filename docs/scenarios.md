# Scenarios: Greenfield, Brownfield, Ambiguous

All three run through the **same** orchestrator and the **same** URL-shortener
codebase — that itself is part of the demonstration: one governed system, three
different kinds of engineering work.

---

## Scenario 1 — Greenfield

**Input**
```json
{
  "scenario": "GREENFIELD",
  "requirement": "Build a URL shortener that allows users to create short links and track clicks."
}
```

**Graph executed**
```
requirements
   │
   ├──────────────┐
   ▼               ▼
architecture   planning          ← runs in parallel
   │               │
   └──────┬────────┘
          ▼
   implementation
          │
   ┌──────┴──────┐
   ▼             ▼
testing       security            ← runs in parallel
   │             │
   └──────┬──────┘
          ▼
   documentation
          │
          ▼
   release review → human approval (MEDIUM→HIGH once release node hit)
          │
          ▼
         done
```

**What it demonstrates**
- decomposition into a real dependency graph (not a linear pipeline)
- genuine parallel execution + synchronization at the `implementation` and
  `documentation` join points
- artifact lineage: `RequirementSpec v1 → ArchitectureSpec v1 → Implementation v1 →
  TestReport v1 → SecurityReport v1 → ReleaseManifest v1`
- a decision captured with rationale, e.g.:
  ```json
  {
    "decision": "Use Base62 for short codes",
    "rationale": "Compact representation, sufficient keyspace, URL-safe alphabet",
    "alternatives": ["UUID", "Hashids"],
    "selected": "Base62",
    "source_artifact": "ArchitectureSpec v1"
  }
  ```
- human approval gate before release

---

## Scenario 2 — Brownfield (primary demo)

**Input**
```json
{
  "scenario": "BROWNFIELD",
  "requirement": "Add configurable URL expiration without breaking existing clients."
}
```

**Graph executed**
```
requirement
   │
   ▼
codebase / impact analysis     ← new: reasons about the EXISTING system first
   │
   ▼
affected: Url entity, urls table, CreateUrl API,
          RedirectService, unit tests, docs
   │
┌──┴───────────┬─────────────┐
▼              ▼             ▼
schema      implementation  test plan (regression, not net-new)
   │              │             │
   └──────┬───────┴─────────────┘
          ▼
    regression tests
          │
          ▼
       security
          │
          ▼
   human approval (schema change = HIGH risk)
          │
          ▼
        release
```

**What it demonstrates**
- codebase reasoning: the `ImpactAnalysisAgent` walks existing artifacts/entities
  and produces an explicit affected-component list *before* any code changes —
  this is the difference between "generate new code" and "safely change a system
  you didn't just build"
- only affected nodes re-run; unrelated prior artifacts (e.g. analytics artifacts)
  stay `SUCCEEDED` and untouched
- schema change correctly triggers `HIGH` risk → human approval, per the risk
  table in `01-architecture.md` §4
- regression testing, not just new-feature testing

### Scenario 2b — Brownfield-on-brownfield: Kafka enhancement (stretch goal)

Second, later requirement against the *same* running system:

```json
{
  "scenario": "BROWNFIELD",
  "requirement": "Move click-event processing to async Kafka processing; redirects must stay fast; existing clients/APIs must not change."
}
```

Flow:
```
requirement
   │
   ▼
impact analysis → affected: RedirectService, ClickTrackingService, EventPublisher
   │                (NOT affected: UrlController, CreateUrl API, analytics read API)
   ▼
architecture change proposal (introduce KafkaEventPublisher behind EventPublisher)
   │
   ▼
human approval (introducing new infra into prod path = HIGH)
   │
   ▼
implementation ──┬── unit tests
                  ├── Kafka integration tests (Testcontainers)
                  └── idempotency tests (duplicate event handling)
   │
   ▼
regression + security + performance validation
   │
   ▼
release (behind analytics.processing.mode=SYNC|ASYNC flag — rollback = flip flag back)
```

This is the payoff of ADR-3: the `EventPublisher` seam means this is a bounded,
reversible brownfield change, not a rewrite. If time allows, include a deliberate
validation failure here to also demonstrate **rollback**:
```
Kafka integration → validation FAIL (consumer processing lag exceeds threshold)
   → rollback: analytics.processing.mode reverted to SYNC
   → workflow re-planned
   → audit event: ROLLBACK_STARTED, reason="latency SLO breach"
```

---

## Scenario 3 — Ambiguous

**Input**
```json
{
  "scenario": "AMBIGUOUS",
  "requirement": "Make the URL shortener highly scalable and improve analytics performance."
}
```

**Graph executed**
```
requirement
   │
   ▼
ambiguity detection            ← RequirementsAgent does NOT proceed to design
   │
   ▼
CLARIFICATION_REQUIRED (workflow parks, not blocked/failed)
   │
   ▼
human input (simulated in demo, e.g. via approval-style API)
   │
   ▼
requirement v2 (normalized, versioned)
   │
   ▼
impact analysis → re-plan
   │
   ▼
architecture → implementation → testing → security → docs → release
```

**Structured ambiguity output** (what `RequirementsAgent` actually returns —
this is the artifact, not just a talking point):
```json
{
  "ambiguities": [
    "What traffic volume defines 'highly scalable'?",
    "What latency target for redirects?",
    "What availability target (99.9%? 99.99%)?",
    "Which analytics queries need to be faster, and how much faster?",
    "What consistency requirements for analytics (eventual OK?)",
    "What data retention period applies?"
  ],
  "status": "CLARIFICATION_REQUIRED"
}
```

**What it demonstrates**
- an agent that recognizes when it doesn't have enough information and stops,
  rather than hallucinating assumptions and coding against them
- the workflow state machine has an explicit `CLARIFICATION_REQUIRED`/
  `WAITING_FOR_APPROVAL`-style parked state, not just `RUNNING`/`FAILED`
- re-planning driven by a requirement version bump, reusing the exact same
  `ImpactAnalyzer` used in the brownfield scenario

---

## Demo script (what the panel should see, ~10–15 min)

1. `docker compose up` — app + Postgres come up, Flyway migrations run.
2. `POST /api/v1/workflows` with the Greenfield payload → watch it complete
   end-to-end, hit the approval gate, approve via curl/Swagger, watch it release.
   Show the artifact lineage and metrics summary endpoint.
3. Run Brownfield — pause at the `WAITING_FOR_APPROVAL` gate on the schema change,
   show the impact-analysis artifact naming exactly which components are affected
   (and which aren't).
4. Run Ambiguous — show it parking at `CLARIFICATION_REQUIRED` with the structured
   ambiguity list, then feed a clarified requirement and watch selective re-plan.
5. Force one failure deliberately (e.g. a failing test) to show retry → retry →
   fallback/rollback → audit trail, live.
6. Hit `/api/v1/workflows/{id}/audit` and the metrics endpoint to show
   success rate, retry count, rollback count, MTTR, end-to-end latency for the run.

Steps 3–5 are the ones that actually differentiate the submission — don't let the
happy-path greenfield demo eat the panel's attention budget.
