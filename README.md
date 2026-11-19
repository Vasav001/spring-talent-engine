# spring-talent-engine

Multi-tenant AI resume screening platform. Companies upload batches of resumes against a job description; the app extracts a skill rubric, screens every resume with a keyword gate plus one tool-calling AI agent, **checks the AI's evidence in plain Java**, and returns a ranked, explainable shortlist that recruiters can override.

Built with Spring Boot 4, Spring AI 2, Spring Modulith, PostgreSQL row-level security, S3 and SQS. Runs locally on S3/SQS-compatible containers and was deployed to AWS for a one-week demo.

> Screenshots of the AWS deployment and load test: [jump to Screenshots](#screenshots-aws-demo-week)

## Highlights

- **Evidence-backed AI scoring.** The agent must quote the resume word for word for every skill it scores. Java verifies each quote; unsupported claims trigger one retry with feedback, then score 0 and are flagged.
- **Keyword gate before the LLM.** Missing a MUST skill means no LLM call (and no credit used). Synonyms (`k8s` → Kubernetes) and word boundaries (`Java` ≠ `JavaScript`).
- **Real multitenancy.** `tenant_id` on every row **and** PostgreSQL row-level security, with the app connecting as a non-owner database role so RLS cannot be bypassed.
- **Batch pipeline.** Presigned S3 uploads (no file bytes through the app), transactional outbox → SQS, rate-limit-aware worker with retries and a dead-letter queue, idempotent processing, live progress over SSE.
- **Billing.** Plans, 30-day periods, fake payment gateway behind an interface, append-only credit ledger with row-locked reservation and refunds.
- **BYOK.** Tenants can bring their own OpenAI-compatible key: AES-GCM encrypted, masked everywhere, SSRF-checked base URL, per-tenant AI client.
- **Human in the loop.** Recruiters edit the rubric before publishing and can override any score with a reason; every override is kept in history.
- **Model independent.** One OpenAI-compatible client; tested with Groq (`openai/gpt-oss-120b`).

## Architecture

```mermaid
flowchart LR
    client[Recruiter / Postman] -->|HTTPS + Auth0 JWT| caddy[Caddy TLS]
    client -->|presigned PUT| s3[(S3 / RustFS)]
    caddy --> app

    subgraph app [Spring Boot modular monolith]
        identity --> tenant
        jobs
        billing
        ingestion
        screening
    end

    app --> pg[(PostgreSQL + RLS)]
    app --> s3
    ingestion -->|outbox → message| sqs[[SQS / ElasticMQ]]
    sqs -->|worker| screening
    screening -->|OpenAI-compatible API| llm[Groq or tenant's own key]
    app -.->|JWKS| auth0[Auth0]
```

| Concern | Local | AWS (demo week) |
|---|---|---|
| Files | RustFS (S3 API) | S3 |
| Queue | ElasticMQ (SQS API) | SQS + dead-letter queue |
| Database | PostgreSQL 17 | PostgreSQL 17 on the EC2 instance |
| Compute | your PC | one EC2 `t4g.small`, Docker Compose, Caddy |

Same code everywhere; only endpoints change by Spring profile.

### Modules (Spring Modulith)

```mermaid
flowchart BT
    shared
    tenant --> shared
    identity --> tenant
    jobs --> tenant
    billing -. listens to TenantCreated .-> tenant
    ingestion --> jobs
    ingestion --> billing
    screening --> ingestion
    screening --> jobs
    screening --> billing
    screening --> tenant
```

| Module | Responsibility |
|---|---|
| `tenant` | Tenants, members, roles, BYOK settings |
| `identity` | JWT → current user, tenant resolution, role → permissions |
| `jobs` | Job descriptions, LLM rubric extraction, rubric editing |
| `billing` | Plans, subscriptions, fake payments, credit ledger |
| `ingestion` | Single and batch uploads, text extraction, PII masking, queueing |
| `screening` | Keyword gate, AI agent, evidence verification, ranking, overrides, usage |
| `shared` | Tenant context, current user, tenant-aware data source |

`ApplicationModules.verify()` runs as a test, so cycles or access to another module's internals fail the build. The same test writes PlantUML diagrams to `target/spring-modulith-docs`.

### Screening one resume

```mermaid
sequenceDiagram
    participant W as Worker (SQS listener)
    participant I as ingestion
    participant G as Keyword gate
    participant A as AI agent
    participant V as Java verifier
    W->>I: prepare(resumeId): download, extract text, mask PII, dedupe
    W->>G: rubric + text
    alt a MUST skill is missing
        G-->>W: LOW_MATCH (no LLM call, credit refunded)
    else
        W->>A: rubric + resume (+ GitHub tool if a profile is linked)
        A-->>V: score card with quotes
        V-->>A: retry once if a quote is not in the resume
        V-->>W: verified scores
    end
    W->>W: final = 0.3 × keyword + 0.7 × AI, save, mark DONE
```

## Results

| What | Result |
|---|---|
| Eval: 6 hand-ranked resumes vs. the model ranking | pairwise agreement **1.00** |
| Keyword-stuffed resume (all skills listed, no experience) | keyword 100, AI 20 → ranked below a real junior |
| API checks on AWS (uploads, queue, SSE, credits, payments, BYOK, overrides, isolation) | **40/40** |
| 8-file batch on AWS, 2 workers | **19.4 s** from submit to complete |
| **Load test on AWS:** 50 synthetic resumes, 2 workers, Groq free tier | **7.6 min** end to end, **6.6 resumes/min** (5.3 AI screenings/min), 0 failures, 0 queue retries; 50 presigned uploads in 7 s |
| Keyword gate in the load test | skipped 10 of 50 (20 %), exactly the resumes missing a required skill; 10 credits refunded |
| Quality under load | all 35 real Java profiles ranked above all 5 keyword-stuffers (best stuffer: 36th of 50) |
| Tokens per AI-screened resume | about 1,800–2,200 |
| Bottleneck | the LLM provider's free-tier limit (8,000 tokens/min), not the app: steady 5 AI screenings per minute after a first-minute burst of 9 |
| Cost | LLM $0 (Groq free tier); AWS about $4–5 per week, covered by free credits |

## Screenshots (AWS demo week)

All taken during the deployment in `ap-southeast-2` on 2026-11-19; account details are cropped or blacked out.

**One CloudFormation template creates everything** ([`infra/aws/stack.yaml`](infra/aws/stack.yaml)); teardown is one stack delete.

![CloudFormation stacks](docs/screenshots/aws-01-cloudformation-stacks.png)

<img src="docs/screenshots/aws-02-cloudformation-resources.png" alt="CloudFormation resources" width="560">

**Queue during the 50-resume load test.** The 50 messages arrive at once (the transactional outbox sends them after the batch commits), the 2 workers drain them steadily, and nothing reaches the dead-letter queue. Graph times are UTC (17:00 = 22:30 IST).

![SQS queues](docs/screenshots/aws-03-sqs-queues.png)

![SQS monitoring during the load test](docs/screenshots/aws-04-sqs-monitoring-load-test.png)

**The server is not the bottleneck.** CPU on the `t4g.small` peaked at 3.2 % while screening 50 resumes; throughput is capped by the LLM provider's free-tier token limit.

![EC2 CPU during the load test](docs/screenshots/aws-06-ec2-cpu-load-test.png)

![EC2 monitoring](docs/screenshots/aws-05-ec2-monitoring.png)

**Storage.** Files are uploaded straight to S3 with presigned URLs into per-tenant folders (`tenants/{tenantId}/jobs/{jobId}/`). The bucket is private, encrypted, and deletes demo files after 7 days.

![S3 per-tenant folders](docs/screenshots/aws-07-s3-per-tenant-folders.png)

![S3 public access and encryption](docs/screenshots/aws-08-s3-public-access-and-encryption.png)

![S3 lifecycle rule](docs/screenshots/aws-09-s3-lifecycle-7-days.png)

**Access and HTTPS.** The instance role has only Session Manager access plus an inline policy for this bucket and these two queues (no access keys, no SSH port). Caddy serves a Let's Encrypt certificate on a free `sslip.io` name and adds HSTS.

![IAM role policies](docs/screenshots/aws-10-iam-role-policies.png)

<img src="docs/screenshots/aws-11-https-health.png" alt="Health endpoint over HTTPS" width="480"> <img src="docs/screenshots/aws-12-https-response-headers.png" alt="Response headers through Caddy" width="480">

## Tech stack

Java 21 · Spring Boot 4.0.9 · Spring AI 2.0.1 (OpenAI-compatible) · Spring Modulith 2.0.8 · Spring Cloud AWS 4.2.1 · Spring Security resource server (Auth0) · Spring Data JPA, Flyway · PostgreSQL 17 · Apache Tika · springdoc · Testcontainers · Docker Compose · CloudFormation · Caddy.

## Running locally

Requirements: Java 21, Docker, an Auth0 tenant (free) and an OpenAI-compatible API key (for example Groq, free tier).

```bash
docker compose up -d
```

Set the environment variables listed in [`.env.example`](.env.example) (at least `AUTH0_ISSUER_URI`, `AUTH0_AUDIENCE`, `LLM_BASE_URL`, `LLM_API_KEY`, `LLM_MODEL`), then:

```bash
./mvnw spring-boot:run
```

- Flyway creates the schema and a separate non-owner database role for the app.
- Swagger UI: http://localhost:8080/swagger-ui.html
- Auth0 needs a post-login Action that puts the verified email in the access token, see [`docs/PLAN.md`](docs/PLAN.md).

Typical flow: `POST /tenants` → `POST /jobs` → review the rubric → `POST /jobs/{id}/publish` → `POST /jobs/{id}/batches` → upload to the presigned URLs → `POST /batches/{id}/submit` → `GET /batches/{id}/events` → `GET /jobs/{id}/results`. Send `X-Tenant-Id` on every tenant request.

## Tests

```bash
./mvnw test
```

- **Unit tests:** rubric cleaning, keyword gate, evidence verifier, scoring, PII masking, billing rules, BYOK URL check and encryption.
- **AI layer:** a scripted `ChatModel` (no network) tests the agent: one call, retry with feedback, zeroing invented evidence, bad replies, tool only when a GitHub profile is linked.
- **Tenant isolation:** Testcontainers PostgreSQL, app connects as the non-owner role, cross-tenant reads and writes are blocked by RLS.
- **Module boundaries:** `ApplicationModules.verify()`.
- **Eval:** `ScreeningEvalTests` runs only when `LLM_API_KEY` is set and compares the model ranking with a hand ranking.

## Deploying to AWS

Everything lives in [`infra/aws`](infra/aws): a budget stack (alerts on any spend not covered by credits), one CloudFormation stack (EC2, S3, SQS + dead-letter queue, least-privilege IAM role, no SSH port), [`compose.aws.yaml`](compose.aws.yaml) and a `Dockerfile`. Teardown is one stack delete. Step-by-step notes are in [`docs/PLAN.md`](docs/PLAN.md).

## Design decisions

- [ADR 0001: Modular monolith with Spring Modulith](docs/adr/0001-modular-monolith.md)
- [ADR 0002: One AI agent, verified in Java](docs/adr/0002-single-agent-with-java-verification.md)
- [ADR 0003: Shared schema with PostgreSQL row-level security](docs/adr/0003-row-level-security.md)
- [ADR 0004: S3 and SQS APIs with swappable endpoints](docs/adr/0004-swappable-s3-sqs-endpoints.md)
- [ADR 0005: Credits with a ledger and row-locked reservation](docs/adr/0005-billing-and-credits.md)

## Limitations and next steps

- No frontend yet (API only; the planned React app is described in `docs/PLAN.md`).
- The eval set is 6 synthetic resumes; it should grow to about 20 anonymized real ones.
- Payments are simulated; Stripe test mode would be a second `PaymentGateway`.
- Scanned (image-only) PDFs are rejected; no OCR.
