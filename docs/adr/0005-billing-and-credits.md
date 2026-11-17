# ADR 0005: Credits with a ledger and row-locked reservation

**Status:** accepted

## Context
A SaaS needs plans and usage limits, but real payments are out of scope. LLM calls are the expensive part, and concurrent batch submissions must not overspend a tenant's credits.

## Decision
- **Plans as a Java enum:** FREE ($0, 50 credits), PRO ($29, 1,000), BYOK ($9, unlimited, tenant's own key). They change only with a deploy, so no table.
- **Subscriptions:** one per tenant, created with the tenant through a synchronous `TenantCreated` listener. 30-day periods are checked **when read** (no scheduler): an expired period becomes a fresh FREE period.
- **Payments:** a `PaymentGateway` interface with a fake implementation; the "webhook" call locks the payment row and only changes a `PENDING` payment, so replays never grant twice.
- **Credits:** `credits_remaining` plus an append-only `credit_ledger` (`GRANT`, `RESERVE`, `REFUND`), written in the same transaction. Reservation locks the subscription row (`SELECT ... FOR UPDATE`), checks the balance and decrements it; not enough credits is HTTP 402.
- **What costs a credit:** one AI screening. Batch submit reserves one per uploaded file; resumes that skip the AI (low match, duplicate, unreadable) are refunded on their first `QUEUED → DONE/FAILED` transition, so refunds are idempotent too.
- **BYOK:** no reservation; the key is AES-GCM encrypted, shown only as `****` + last 4, and the base URL must be public `https` (SSRF guard).

## Alternatives considered
- **Conditional `UPDATE ... WHERE credits >= n`:** also atomic, but the period check has to happen first anyway, so one row lock covers both.
- **Counting usage from results:** no audit trail and no way to explain a balance.
- **Stripe test mode now:** more setup with no extra learning for this project; it fits later as a second `PaymentGateway`.

## Consequences
- The ledger always explains the balance (verified end to end: reserve 7, refund 3, balance 46).
- No background jobs to run or monitor.
- Token usage is shown per period but not billed.
