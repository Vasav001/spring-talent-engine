# spring-talent-engine — Project Plan

> Multi-tenant AI resume screening platform: Spring Boot, Spring AI (provider-agnostic, BYOK), Spring Modulith, S3/SQS batch pipeline.

**Status:** Final v1 (2026-11-05) · Repo: https://github.com/Vasav001/spring-talent-engine
Scope changes go in section 11 ("Later / maybe") unless they make the app simpler.

---

## 1. Goal

Companies (tenants) upload batches of resumes against a job description. The system:

1. Extracts a skill rubric from the job description (the recruiter reviews and edits it).
2. Gates each resume with cheap keyword matching.
3. Screens resumes that pass with a single tool-calling AI agent, which produces an evidence-backed score card.
4. Checks the AI output with plain Java code.
5. Ranks candidates; recruiters can override scores.

**Design principles**
- Simple enough to explain every part in an interview.
- Spring does the heavy lifting; the AI is one well-built piece.
- Model-independent (any OpenAI-compatible API).
- Runs locally on a low-end PC; deployed to AWS only for a one-week demo.

**Non-goals (for now):** microservices, multi-agent systems, real payments, local LLMs, embeddings/vector search, resume generation.

---

## 2. Architecture

**Style:** modular monolith (Spring Modulith). Modules communicate through application events and published APIs only. `ApplicationModules.verify()` runs in a test.

### Modules

| Module | Responsibility |
|---|---|
| `tenant` | Tenants, members, roles, BYOK settings |
| `identity` | JWT → current user; resolves the tenant context + permissions per request |
| `billing` | Plans, subscriptions, fake payments, credit balance + ledger |
| `jobs` | Job descriptions, rubric extraction, rubric editing |
| `ingestion` | Batches, presigned uploads, text extraction, enqueueing work |
| `screening` | Keyword gate, AI agent, output verification, scoring |
| `review` | Ranked results, human overrides |
| `usage` | Token usage tracking, usage dashboard data |
| `shared` | Tenant context holder, common types, error handling |

### Infrastructure (one code path, endpoint swapped by profile)

| Purpose | `local` profile | `aws` profile (demo week) |
|---|---|---|
| File storage | RustFS (S3 API) | S3 |
| Queue | ElasticMQ (SQS API) | SQS (+ dead-letter queue) |
| Database | Postgres (Docker) | Postgres (Docker on EC2) — no RDS |
| LLM | Groq / OpenRouter / Gemini via OpenAI-compatible API | Same |
| Auth | Auth0 free tier (any OIDC IdP works) | Same |
| Compute | Your PC | 1× EC2 `t4g.small` + Docker Compose + Caddy (TLS) |

---

## 3. Multitenancy & access control

### 3.1 Tenant isolation (shared schema + RLS)
- **IdP handles identity only.** The access token carries the user's **verified email** in a custom claim (Auth0 post-login Action, see Phase 0). Spring uses it as the principal name (`principal-claim-name`). Tenancy and roles live in our database, keyed by email, so the IdP can be swapped without code changes.
- **Tenant selection:** `X-Tenant-Id` header → `TenantResolutionFilter` checks membership → stores the tenant in `TenantContext` (thread-local) and adds the role's permissions as authorities.
- **Shared schema:** every tenant-owned table has a `tenant_id` column; services set it from `TenantContext` on insert.
- **Postgres Row-Level Security** does the filtering:
  - Two DB roles: Flyway migrates as the owner (`talent`); the app connects as `talent_app` (non-owner, so RLS applies). The app role is created by migration `V2` using Flyway placeholders.
  - `TenantAwareDataSource` runs `set_config('app.tenant_id', …)` and `set_config('app.user_email', …)` on **every connection checkout**. Each checkout overwrites the previous values, so a pooled connection can never carry another tenant's context.
  - Tenant-owned tables: one policy `FOR ALL USING (tenant_id = current tenant)`.
  - `tenant_members` adds a read-only policy `email = current user`, so a user can list their own memberships before choosing a tenant.
  - Creating a tenant binds the current transaction to the new tenant (`set_config(..., true)`) so the owner membership passes the insert policy.
  - Integration tests (`TenantIsolationTests`): app runs as `talent_app`; tenant A sees only A's rows; inserting into tenant B fails.
- **Async workers** set `TenantContext` from the queue message before touching the database.
- **S3 keys** are prefixed per tenant: `tenants/{tenantId}/batches/{batchId}/{resumeId}.pdf`.

### 3.2 RBAC
A user has one role **per tenant** (`tenant_members.role`). Roles are mapped to permissions **in code** (a Java enum map), not in DB tables.

| Role | Permissions |
|---|---|
| `OWNER` | Everything, including `BILLING_MANAGE`, `BYOK_MANAGE`, `MEMBER_MANAGE`, `JOB_MANAGE_ALL` |
| `RECRUITER` | `JOB_READ`, `JOB_WRITE`, `BATCH_UPLOAD`, `RESULT_OVERRIDE`, `RESULT_READ` |
| `VIEWER` | `JOB_READ`, `RESULT_READ` |

- On each request: role for the selected tenant → Spring Security authorities → `@PreAuthorize("hasAuthority('JOB_WRITE')")`.
- Roles are **not** put in the JWT (they depend on the tenant).

### 3.3 Ownership rule (the one user-level permission)
- A `RECRUITER` can edit or delete only jobs they created; `OWNER` can edit any job (`JOB_MANAGE_ALL`).
- Implemented as `@PreAuthorize("@jobAccess.canEdit(#jobId)")`. A job from another tenant is invisible (RLS), so it returns 404 instead of revealing that it exists.
- An `OWNER` cannot change their own role (prevents locking the tenant out).

---

## 4. Billing: plans, subscriptions, fake payments, credits

### 4.1 Plans (seed data)
| Plan | Price / month | Credits / month | LLM key |
|---|---|---|---|
| FREE | $0 | 50 | Platform key |
| PRO | $29 | 1,000 | Platform key |
| BYOK | $9 | Unlimited | Tenant's own key + base URL + model |

1 credit = 1 resume screened by the AI. Resumes stopped by the keyword gate are free.

### 4.2 Subscriptions
- One subscription per tenant. Every new tenant starts on FREE.
- The OWNER picks a plan → paid plans go through fake checkout → on success the plan is active for **30 days** with fresh credits.
- **No scheduler, no auto-renewal.** Whenever the subscription is read and its 30 days are over:
  - FREE → start a new 30-day period and reset credits.
  - Paid plan → fall back to FREE (the owner buys again to continue).
- No cancel/downgrade flows and no proration — picking FREE is the downgrade.

### 4.3 Fake payments
- `PaymentGateway` interface → `FakePaymentGateway` implementation (Stripe test mode can be a second implementation later).
- Checkout → a fake payment page with **"Simulate success"** / **"Simulate failure"** buttons. No card fields.
- The result is handled like a webhook: idempotent by payment id → publishes `PaymentSucceeded` / `PaymentFailed` events → billing activates the plan and grants credits.

### 4.4 Credits
- Balance column `subscriptions.credits_remaining` + append-only `credit_ledger` (`GRANT / RESERVE / REFUND`), both written in the same transaction.
- **Atomic reservation** when a batch is submitted:
  `UPDATE subscriptions SET credits_remaining = credits_remaining - :n WHERE tenant_id = :id AND credits_remaining >= :n` → 0 rows updated means reject the batch.
  Credits are refunded for gated or failed resumes. BYOK skips reservation.

### 4.5 BYOK
- **BYOK security:** AES-GCM encryption at rest (`spring-security-crypto`, master key from an environment variable), never logged, and the UI shows only the last 4 characters.
- **Per-tenant `ChatClient`:** built at runtime from the tenant's base URL, key and model; cached per tenant and evicted when settings change.
- Tokens in/out are recorded for every call (taken from the Spring AI response metadata) and shown on the usage page, but they are not billed.

---

## 5. Screening pipeline

### 5.1 Job creation (once per job)
1. The recruiter pastes the job description.
2. **One LLM call** (temperature 0) → `ExtractedRubric` record (structured output via `.entity(...)`): skills, `MUST`/`NICE`, weights 1–5, minimum years. The LLM call runs outside any DB transaction.
3. `Rubric.clean(...)` normalizes skills through the `skill_synonyms` table (`k8s → Kubernetes`), clamps values, merges duplicates (MUST wins) and keeps at most 20 items. The same cleaning runs on manual edits.
4. If extraction fails, the job is still created with an empty rubric; the recruiter retries (`extract-rubric`) or edits manually.
5. **The recruiter reviews and edits the rubric** (human in the loop), then publishes the job. Only `DRAFT` jobs can change; publishing needs at least one requirement.

### 5.2 Batch upload
1. `POST /batches` {jobId, fileNames[]} → reserve credits → returns **presigned PUT URLs**.
2. The browser uploads directly to S3/RustFS.
3. `POST /batches/{id}/complete` → one queue message per resume `{tenantId, batchId, resumeId}`.
4. Progress is pushed to the UI over **SSE** (`processed / failed / total`).

### 5.3 Upload (ingestion module, Phase 3: one file per request)
1. `POST /jobs/{jobId}/resumes` (multipart, PDF/DOCX, max 5 MB); the job must be `PUBLISHED`.
2. **Duplicate check:** SHA-256 content hash, unique per job → 409.
3. **Extract text** with Spring AI's `TikaDocumentReader`; blank text (scanned PDF) → 422. Text capped at 30,000 characters.
4. Read the GitHub username from the raw text, then **mask PII** (email, phone numbers with 10+ digits) before storing the text.
5. Store the original file in S3/RustFS (`tenants/{tenantId}/jobs/{jobId}/{uuid}.pdf`) and the masked text in `resumes`.

### 5.4 Screening (screening module; synchronous in Phase 3, called by the queue worker from Phase 4)
1. **Keyword gate** (`KeywordGate`, pure Java): each rubric skill plus its synonyms, matched on word boundaries (so "Java" doesn't match "JavaScript") → `keywordScore` = matched weight / total weight.
   - Any MUST skill missing → `LOW_MATCH`, LLM skipped. `?force=true` runs the agent anyway.
2. **AI agent** (`ScreeningAgent`: one `ChatClient` call with a tool) → `AgentScoreCard`:
   - For each rubric item: score 0–5 + **evidence quote** copied word-for-word from the resume.
   - Strengths, gaps, recommendation (`STRONG_YES / YES / MAYBE / NO`), one-sentence summary.
3. **Verify the output in Java** (`ScoreCardVerifier`): every evidence quote with score > 0 must appear in the resume (case, quotes, bullets and whitespace normalized; `...` splits a quote into parts).
   - If any fail → **one retry** with feedback naming the skills → anything still unsupported gets score 0 and `verified: false`.
4. **Scores:** `aiScore` = weighted average of criterion scores (0–100); **final score** = `0.3 × keywordScore + 0.7 × aiScore` (constant in `ScreeningOutcome`). `LOW_MATCH` gets `0.3 × keywordScore` so it always ranks below screened candidates.
5. Save one `screening_results` row per resume (re-screening overwrites it) with the report as `jsonb`, model name and token counts. AI errors → status `FAILED` with a generic message; details go to the log.
6. From Phase 4: failures retried by the queue → dead-letter queue after 3 attempts → `FAILED`, credit refunded.

### 5.5 Agent tool (`@Tool` method; the model decides whether to call it)
| Tool | Purpose |
|---|---|
| `getGithubSummary(username)` | Own public repos, top languages, recently pushed repos (GitHub REST API, 5 s timeout, optional `GITHUB_TOKEN`). Only called when the resume links a GitHub profile; used for strengths/gaps, never as evidence. |

A `normalizeSkill` tool was dropped: synonyms are applied in Java before and after the LLM call, so the tool added nothing.

### 5.5 Model independence
- Uses only the **OpenAI-compatible** Spring AI starter. Switching provider changes only `base-url`, `api-key` and `model`.
- Candidate providers: Groq, OpenRouter, Google Gemini (OpenAI-compatible endpoint), Cerebras, Mistral.
- The chosen model must support **tool calling**; test this early.
- Free-tier rate limits → keep worker concurrency low (configurable); Spring AI's retry handles 429s.

---

## 6. Data model

| Table | Key columns |
|---|---|
| `tenants` | id, name, created_at |
| `tenant_members` | id, tenant_id, email, role (OWNER/RECRUITER/VIEWER), created_at — unique (tenant_id, email) |
| `plans` (global, seeded) | code, name, monthly_price, monthly_credits (null = unlimited), byok |
| `subscriptions` | id, tenant_id, plan_code, period_start, period_end, credits_remaining |
| `payments` | id, tenant_id, subscription_id, plan_code, amount, currency, status (PENDING/SUCCEEDED/FAILED), provider (FAKE), created_at |
| `credit_ledger` | id, tenant_id, type (GRANT/RESERVE/REFUND), amount, batch_id, resume_id, created_at |
| `tenant_llm_settings` | tenant_id, base_url, model, api_key_encrypted, api_key_last4 |
| `skill_synonyms` (global) | alias, canonical |
| `jobs` | id, tenant_id, title, description, status (DRAFT/PUBLISHED/CLOSED), created_by (email) |
| `job_requirements` | id, tenant_id, job_id, skill, type (MUST/NICE), weight, min_years |
| `batches` | id, tenant_id, job_id, total, processed, failed, status, created_by |
| `resumes` | id, tenant_id, batch_id, file_name, storage_key, content_hash, status (UPLOADED/QUEUED/SCREENED/LOW_MATCH/FAILED), github_username |
| `screening_results` | id, tenant_id, resume_id, keyword_score, ai_score, final_score, recommendation, scorecard (jsonb), model, tokens_in, tokens_out |
| `review_overrides` | id, tenant_id, result_id, reviewer_email, override_score, reason, created_at |
| `usage_records` | id, tenant_id, resume_id, tokens_in, tokens_out, provider, model, created_at |
| `event_publication` | managed by Spring Modulith (transactional outbox) |

Schema managed by Flyway migrations.

---

## 7. API sketch

```
GET    /me                                  current user + tenants
POST   /tenants                             create tenant (caller becomes OWNER)
POST   /tenants/members                     invite member (OWNER)
PUT    /tenants/members/{id}/role           change role (OWNER)
PUT    /tenants/llm-settings                BYOK settings (OWNER)
GET    /tenants/usage                       credits + token usage

GET    /plans                               list plans
GET    /billing/subscription                current plan, period end, credits
POST   /billing/checkout                    {planCode} → fake checkout session (OWNER)
POST   /billing/fake-gateway/{paymentId}    {result: SUCCESS|FAILURE} simulate payment
GET    /billing/ledger                      credit history

POST   /jobs                                create job (triggers rubric extraction)
GET    /jobs?status=&page=&size=            paged job list
GET    /jobs/{id}                           job + rubric
PUT    /jobs/{id}/requirements              replace rubric (draft only)
POST   /jobs/{id}/extract-rubric            re-run LLM extraction (draft only)
POST   /jobs/{id}/publish                   DRAFT → PUBLISHED
POST   /jobs/{id}/close                     PUBLISHED → CLOSED
DELETE /jobs/{id}                           delete draft

POST   /batches                             reserve credits, get presigned URLs
POST   /batches/{id}/complete               enqueue processing
GET    /batches/{id}/events        (SSE)    progress stream
POST   /jobs/{id}/resumes          (multipart) upload one resume
POST   /resumes/{id}/screen?force=          screen now (force bypasses the keyword gate)
GET    /jobs/{id}/results?status=&page=     ranked candidates (final score, highest first)
GET    /results/{id}                        full report with evidence
POST   /results/{id}/override               human override
```

---

## 8. Frontend (minimal)

React + Vite + TypeScript, Auth0 SPA SDK, TanStack Query. Pages: login → tenant picker → jobs → rubric editor → batch upload (with progress bar) → ranked results → score card detail → settings (BYOK, plan, usage).
On AWS, Caddy serves it as static files on the same EC2 instance.

---

## 9. Phases

### Phase 0 — Setup
- [x] Repo `spring-talent-engine`, Maven project generated (Boot 4.0.9, base package `com.vasav.springtalentengine`)
- [x] Align `pom.xml` with section 12 (removed Spring Batch, added Spring Cloud AWS S3/SQS)
- [ ] Add test dependencies when needed (WireMock, Awaitility, LocalStack)
- [x] Package-per-module layout + `ModularityTests`
- [x] `compose.yaml`: Postgres, RustFS (+ bucket via AWS CLI init container), ElasticMQ (+ queue and dead-letter queue)
- [x] `application.yaml` + `local` / `aws` profiles, secrets from environment variables (IntelliJ run config; names listed in `.env.example`)
- [x] Flyway `V1__event_publication.sql` (Modulith outbox table)
- [ ] Auth0 tenant + SPA app + API audience
- [ ] Auth0 post-login Action that puts the verified email into the access token:
  ```js
  exports.onExecutePostLogin = async (event, api) => {
    if (event.user.email_verified) {
      api.accessToken.setCustomClaim('https://spring-talent-engine/email', event.user.email);
    }
  };
  ```
- [ ] Free LLM API key (Groq or OpenRouter); confirm the model supports tool calling

### Phase 1 — Tenancy foundation
- [x] JWT resource server (email claim as principal), `/me`
- [x] Tenants, members, roles, `TenantResolutionFilter` + `TenantContext`
- [x] RBAC: role → permissions map, `@PreAuthorize` checks (job ownership rule moves to Phase 2)
- [x] Postgres RLS + non-owner app role + `TenantIsolationTests`
- [x] `ApplicationModules.verify()` test
- [ ] Run on the Docker PC: app starts, `TenantIsolationTests` pass, try `/me` and `/tenants` with a real Auth0 token

### Phase 2 — Jobs & rubric
- [x] Job CRUD + paged list, RLS on `jobs` and `job_requirements`
- [x] LLM rubric extraction (structured output)
- [x] Synonym table (seeded) + normalization, `RubricTests`
- [x] Rubric edit + publish/close, job ownership rule (`JobAccess`)
- [ ] Run on the Docker PC: create a job with a real JD, check the extracted rubric, edit, publish

### Phase 3 — Single-resume screening (end to end, synchronous)
- [x] Upload, Tika text extraction + PII masking, file in S3/RustFS, duplicate check
- [x] Keyword gate
- [x] Agent with GitHub tool → `AgentScoreCard`
- [x] Evidence verification + retry
- [x] Results API (ranked list + full report), `PiiMaskerTests`, `ScreeningLogicTests`
- [ ] Run on the Docker PC: upload 3–5 real resumes, screen them, check evidence and ranking

### Phase 4 — Batch pipeline
- [ ] Presigned uploads (RustFS)
- [ ] Queue fan-out (ElasticMQ), configurable concurrency, dead-letter queue
- [ ] Idempotency + content-hash dedup
- [ ] SSE progress

### Phase 5 — Billing & BYOK
- [ ] Plans seed data, FREE subscription created with each new tenant
- [ ] Fake checkout + simulate success/failure + `PaymentSucceeded` / `PaymentFailed` events
- [ ] Credit ledger + atomic reservation + refunds
- [ ] 30-day period check on read (reset FREE credits, expire paid plans to FREE)
- [ ] Encrypted BYOK settings + per-tenant ChatClient cache
- [ ] Usage page (credits, tokens)

### Phase 6 — Review & quality
- [ ] Human overrides
- [ ] Small eval set: ~20 resumes ranked by hand + a JUnit test checking ranking agreement
- [ ] WireMock-based tests for the AI layer (deterministic fake responses)

### Phase 7 — AWS demo week
- [ ] **AWS Budgets alerts at $5 / $15 / $30 before creating anything**
- [ ] S3 bucket (private, CORS for presigned PUT), SQS queue + dead-letter queue
- [ ] EC2 `t4g.small` (public subnet, no NAT, no ALB), Docker Compose, Caddy TLS
- [ ] IAM instance role (no access keys on the box)
- [ ] Run a real batch (50–100 resumes); record numbers: throughput, cost/resume, gate skip rate
- [ ] Screenshots, a 2–3 min demo video, CloudWatch/console screenshots as proof
- [ ] **Teardown:** terminate EC2, delete S3 bucket + SQS queues, release Elastic IP, delete snapshots/volumes, check billing after 24h

### Phase 8 — Docs
- [ ] README: problem, architecture diagram, module diagram (Spring Modulith docs), key design decisions, demo video link
- [ ] ADRs for: modular monolith, single agent + code verification, RLS, endpoint-swap infrastructure, billing + credits model

---

## 10. Interview talking points

- **Single agent + verification in code** instead of multi-agent: cheaper, testable, deterministic checks.
- **Keyword gate + LLM:** keywords are cheap and explainable; the LLM handles context. The gate saves quota.
- **Defense-in-depth tenancy:** app-level `tenant_id` + Postgres RLS (transaction-scoped setting) + per-tenant S3 prefixes.
- **Per-tenant RBAC:** roles live in the DB per tenant, permissions in code; one ownership rule for record-level access.
- **Billing behind an interface:** fake gateway now, Stripe later; payment results handled idempotently through events.
- **Atomic credit reservation + ledger:** concurrent batch submissions can't overspend, and every credit change is audited.
- **Queue-based batching:** rate-limit-aware concurrency, retries, dead-letter queue, idempotency.
- **Portable infrastructure:** S3/SQS APIs with RustFS/ElasticMQ locally — the same code runs on and off AWS.
- **Model independence + BYOK:** one OpenAI-compatible client, configured per tenant at runtime.

---

## 11. Later / maybe

- Candidate self-check mode (resume + job description → same score card + gap list)
- MCP server exposing the screening tools (`spring-ai-starter-mcp-server-webmvc`)
- Embedding pre-filter with pgvector
- Recruiter chat assistant over candidates
- ZIP batch upload
- Stripe test mode as a second `PaymentGateway` implementation

---

## 12. Dependencies

**Versions** (checked against start.spring.io and Maven Central on 2026-11-05):

| Component | Version | Note |
|---|---|---|
| Java | 21 (LTS) | |
| Spring Boot | **4.0.9** | Chosen because Spring Cloud AWS 4.x officially supports Boot 4.0.x |
| Build | Maven | |

**Versions you must set yourself** (not managed by Spring Boot):

| Artifact | Version | How |
|---|---|---|
| `org.springframework.ai:spring-ai-bom` | **2.0.1** | BOM import (supports Boot >= 4.0.0 and < 4.2.0-M1) |
| `org.springframework.modulith:spring-modulith-bom` | **2.0.8** | BOM import (the line for Boot 4.0.x) |
| `io.awspring.cloud:spring-cloud-aws-dependencies` | **4.2.1** | BOM import (4.x = Spring Cloud 2025.1 / Boot 4.0.x) |
| `org.springdoc:springdoc-openapi-starter-webmvc-ui` | **3.0.3** | Direct dependency (the line for Boot 4.0.x) |
| `org.wiremock:wiremock-standalone` | **3.13.2** | Direct dependency, test scope (latest stable 3.x) |

Everything else gets its version from a BOM — no version tag needed:
- **Spring Boot manages:** all `spring-boot-starter-*`, `postgresql` (42.7.14), `flyway-database-postgresql` (Flyway 11.14.1), `testcontainers-*` (2.0.5), `awaitility` (4.3.0), `lombok`.
- **Spring AI BOM manages:** `spring-ai-starter-model-openai`, `spring-ai-tika-document-reader` (and Tika itself).
- **Modulith BOM manages:** `spring-modulith-starter-*`.
- **Spring Cloud AWS BOM manages:** `spring-cloud-aws-starter-s3`, `spring-cloud-aws-starter-sqs`.

### Main
| Area | Artifact | Why |
|---|---|---|
| Web | `spring-boot-starter-webmvc` | REST + SSE |
| Validation | `spring-boot-starter-validation` | Request DTO validation |
| Ops | `spring-boot-starter-actuator` | Health, metrics |
| Security | `spring-boot-starter-security`, `spring-boot-starter-security-oauth2-resource-server` | Validate Auth0 JWTs (includes `spring-security-crypto` for BYOK encryption) |
| Data | `spring-boot-starter-data-jpa` | JPA / Hibernate (`@TenantId`) |
| DB driver | `org.postgresql:postgresql` (runtime) | Postgres |
| Migrations | `spring-boot-starter-flyway` + `flyway-database-postgresql` | Schema + RLS policies |
| Modulith | `spring-modulith-starter-core` | Module boundaries, events |
| Modulith | `spring-modulith-starter-jpa` | Event publication registry (outbox) |
| Modulith (runtime) | `spring-modulith-actuator`, `spring-modulith-observability`, `spring-modulith-runtime` | Module info on actuator, per-module tracing |
| Boilerplate | `lombok` (optional) | Getters/builders on JPA entities; records for DTOs |
| AI | `spring-ai-starter-model-openai` | Any OpenAI-compatible provider (Groq, OpenRouter, Gemini…) |
| AI | `spring-ai-tika-document-reader` | PDF/DOCX → text |
| AWS | `spring-cloud-aws-starter-s3` | S3 / RustFS + presigned URLs |
| AWS | `spring-cloud-aws-starter-sqs` | SQS / ElasticMQ listener + sender |
| API docs | `org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.3` | Swagger UI |
| Dev | `spring-boot-docker-compose` (optional, dev only) | Auto-starts `compose.yaml` |

### Test
| Artifact | Why |
|---|---|
| Boot 4 per-starter test starters: `spring-boot-starter-webmvc-test`, `-data-jpa-test`, `-flyway-test`, `-security-test`, `-security-oauth2-resource-server-test`, `-validation-test`, `-actuator-test` | JUnit 5, AssertJ, Mockito, slice tests, mock JWTs |
| `spring-modulith-starter-test` | `ApplicationModules.verify()`, scenario tests |
| `spring-boot-testcontainers` | `@ServiceConnection` |
| `testcontainers-junit-jupiter`, `testcontainers-postgresql`, `testcontainers-localstack` | Real Postgres (RLS tests), S3/SQS |
| `org.wiremock:wiremock-standalone:3.13.2` | Fake LLM HTTP responses |
| `org.awaitility:awaitility` | Assert async queue processing |

### Local infrastructure (`compose.yaml`)
| Service | Image |
|---|---|
| Postgres | `postgres:17.10-alpine` (host port 5433) |
| RustFS | `rustfs/rustfs:1.0.4` (API 9000, console 9001) |
| Bucket init | `amazon/aws-cli:2.39.4` |
| ElasticMQ | `softwaremill/elasticmq-native:1.7.1` |

### Frontend
`react`, `vite`, `typescript`, `@auth0/auth0-react`, `@tanstack/react-query`, `react-router`, a UI kit (e.g. shadcn/ui + Tailwind).

### External accounts (free)
- Auth0 free tier (identity)
- Groq / OpenRouter / Gemini API key (LLM)
- GitHub token (optional; raises the GitHub API rate limit for the agent tool)
- AWS free tier (demo week only)
