# Design Decisions (ADR-style)

Each entry: decision → why → what I'd say if challenged in the interview.

---

### ADR-1: Java 21 + Spring Boot over Python

**Decision:** Java/Spring Boot for the whole system, including the agent layer.

**Why:** This is a Java Developer interview. The core problem being evaluated —
deterministic workflow orchestration, persistence, governance, reliability — is not
an AI-model problem, it's a distributed-systems/software-engineering problem. Java
gives strong typing, mature DI, and a production-grade testing/observability story.

**If asked "isn't Python better for AI?":**
> "For rapid experimentation with agent frameworks, yes. But I separated the LLM
> integration behind an `LlmClient` interface, so the orchestration domain doesn't
> care what implements it. If the AI workload became dominant, I could move it to a
> Python service without touching the orchestrator."

---

### ADR-2: PostgreSQL over MongoDB

**Decision:** PostgreSQL for both the URL-shortener data and the orchestration
state (workflows, nodes, artifacts, decisions, approvals, audit).

**Why:** The dominant access pattern is relational and transactional — a node
`depends_on` other nodes, an artifact is `derived_from` other artifacts, a URL
`has_many` click events. Foreign keys and constraints make these relationships
explicit and enforceable, and I want consistency guarantees around approval and
state-transition writes.

**If asked "why not Mongo for the flexible agent artifacts?":**
> "If artifact payloads became highly heterogeneous or needed document-centric
> retrieval at large scale, I'd reconsider. I store artifact `content` as `jsonb`
> inside Postgres, which gives me schema flexibility for the payload while keeping
> relational integrity for the graph structure around it — the access pattern
> didn't justify a second database."

---

### ADR-3: No Kafka in v1

**Decision:** `EventPublisher` interface, `InProcessEventPublisher` implementation
only. No message broker in the initial build.

**Why:** No workload in v1 requires decoupled async processing — redirect and
click-tracking are fast synchronous operations at this scale. Introducing Kafka
now would add operational complexity (broker, consumer groups, delivery semantics,
idempotency) without demonstrating anything the rubric asks for.

**Designed-for-later:** the `EventPublisher` seam means a `KafkaEventPublisher` can
be swapped in later with zero changes to `RedirectService`/`WorkflowEngine`. This
becomes the brownfield Kafka-enhancement scenario (see `03-scenarios.md`,
Scenario 2b) — a real, justified brownfield change rather than technology for its
own sake.

**If asked "why didn't you use Kafka?":**
> "I evaluated it against the actual workload and couldn't find a requirement that
> justified the operational cost at this stage. I did design the event-publishing
> boundary so it's a config change, not a rewrite, when that requirement shows up —
> which I demonstrate directly in the brownfield scenario."

---

### ADR-4: Modular monolith over microservices

**Decision:** One deployable Spring Boot app with clear internal module
boundaries (`url`, `orchestration`, `agent`, `policy`, `approval`, `audit`).

**Why:** Splitting into services now would introduce network failures,
distributed transactions, and deployment complexity that doesn't buy anything for
a 2–3 day prototype. Module boundaries are drawn deliberately so `orchestration`
and `agent-runtime` could be extracted later if team or scale ever justified it.

---

### ADR-5: LLM behind an interface, mocked by default

**Decision:** `LlmClient` interface. `MockLlmClient` is the default
(`agent.llm.mode=mock`); a real Claude/OpenAI-backed `RealLlmClient` is available
behind `agent.llm.mode=real`.

**Why:** CI and the recorded demo need to be deterministic and reproducible — a
flaky or rate-limited LLM call shouldn't be able to fail the orchestration tests.
Structured JSON output (validated against a Jakarta-Validation-annotated record) is
required from either implementation, so the orchestrator never depends on raw LLM
text.

**If asked "so the AI part is fake?":**
> "The orchestration and governance — which is what's actually being evaluated —
> is fully real and runs the same way regardless of which `LlmClient` is wired in.
> The real-LLM path is available and I can demo it live; it's just not what CI
> depends on, the same way you wouldn't want a real third-party API call in a unit
> test suite."

---

### ADR-6: Orchestrator owns control; agents only propose

**Decision:** `Agent.execute()` returns a recommendation (`AgentResult` with
proposed artifacts/decisions). It never directly calls a mutating tool. All
dependency evaluation, retries, rollback, and policy enforcement live in
`WorkflowEngine` / `PolicyEngine`, not in the agent.

**Why:** This is the single most important architectural decision in the whole
assignment. An LLM must never be able to bypass a policy check or authorize its
own high-risk action — reasoning and governance have different reliability
characteristics, and only one of them should be deterministic.

---

### ADR-7: Rollback is compensating, not transactional

**Decision:** `RollbackAction.compensate()` reverses effects explicitly (mark
artifact invalidated, reverse a patch, run a down-migration) rather than relying on
a database transaction spanning the whole workflow.

**Why:** Agent actions can span external systems (files, git, test runners) that
aren't inside a single DB transaction, so "rollback" has to mean "restore a known
good state," not "undo a commit."

---

### ADR-8: Custom DAG/state-machine instead of Temporal/Camunda

**Decision:** Hand-rolled `WorkflowGraph` + `WorkflowStateMachine`, not an existing
workflow engine.

**Why:** Part of what's being evaluated is whether I understand orchestration
mechanics myself, not whether I can configure a framework. For a real production
system with long-running workflows and many concurrent executions, I would
seriously evaluate Temporal instead of maintaining this engine.

---

## One answer to memorize for the broad "why this stack" question

> "I made these choices based on the requirements, not to maximize the number of
> technologies. Postgres gives transactional consistency for workflow state,
> approvals, artifacts, and audit data. Kafka would matter for a larger
> distributed async architecture, but I couldn't justify its operational cost for
> this workload — so I built the seam for it instead. Java/Spring Boot fits both
> the interview context and the actual problem, which is deterministic workflow
> orchestration and governance, not AI experimentation. The LLM is isolated behind
> an interface so it can be replaced or moved to another language entirely without
> touching the orchestration domain. My principle throughout was: use the simplest
> architecture that satisfies today's requirements, with clear seams for the
> requirements I can already see coming."
