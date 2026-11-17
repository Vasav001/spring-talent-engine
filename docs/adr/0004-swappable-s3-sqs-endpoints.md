# ADR 0004: S3 and SQS APIs with swappable endpoints

**Status:** accepted

## Context
The app must run for free on a low-end PC most of the time and on AWS only for a one-week demo paid by free credits. Batch screening needs file storage and a queue with retries.

## Decision
- Code only against the **S3 and SQS APIs** through Spring Cloud AWS (`S3Template`, `SqsTemplate`, `@SqsListener`).
- **Local profile:** RustFS (S3-compatible) and ElasticMQ (SQS-compatible) by endpoint override, in Docker Compose or as native processes. MinIO was the first choice but its images are no longer published.
- **AWS profile:** no endpoints, credentials from the EC2 instance role, queue must already exist (fail fast).
- **Presigned PUT URLs:** clients upload files directly to storage; the app never handles the bytes of a batch.
- **Queue semantics:** SQS visibility timeout 120 s, dead-letter queue after 3 receives; the worker records `FAILED` itself on the 3rd attempt, and every status change is a conditional update so redelivered messages are no-ops.
- **Outbox:** `BatchSubmitted` is stored with the batch in one transaction (Spring Modulith event registry); messages are sent after commit and resent on restart if the app crashed.

## Alternatives considered
- **Spring Batch:** built for scheduled ETL jobs, not for fan-out of slow, rate-limited API calls.
- **LocalStack:** heavier and more than needed for two services.
- **Database polling as a queue:** simpler but no visibility timeout, dead-letter queue or real SQS experience.

## Consequences
- Same code locally and on AWS; only `application-local.yaml` and `application-aws.yaml` differ.
- On AWS the region must match the stack: a hard-coded fallback once pointed the app at Mumbai while the stack was in Sydney ("queue does not exist"); `AWS_REGION` is now set in `.env`.
