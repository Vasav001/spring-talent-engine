# ADR 0003: Shared schema with PostgreSQL row-level security

**Status:** accepted

## Context
Many companies share one database. One missing `WHERE tenant_id = ?` in any query would leak another company's candidates, which is the worst bug this product can have.

## Decision
- One schema; every tenant-owned table has a `tenant_id` column.
- **Row-level security** on every tenant-owned table: `USING (tenant_id = current_setting('app.tenant_id'))`.
- **Two database roles:** Flyway migrates as the owner; the app connects as `talent_app`, a non-owner role, because table owners bypass RLS. The role is created by a migration using Flyway placeholders.
- `TenantAwareDataSource` sets `app.tenant_id` and `app.user_email` on **every connection checkout**, so a pooled connection can never keep the previous request's tenant.
- The tenant comes from the `X-Tenant-Id` header and is accepted only if the user is a member of that tenant (`TenantResolutionFilter`, otherwise 403). Async workers set the tenant from the queue message.
- Services still filter by `tenant_id` too (defense in depth).

## Alternatives considered
- **Database or schema per tenant:** strong isolation but many migrations and connection pools; too heavy for this size.
- **Application filtering only:** one forgotten filter leaks data, and nothing would catch it.

## Consequences
- A missing filter in Java returns no rows instead of another tenant's rows.
- `TenantIsolationTests` (Testcontainers) prove the app role is not the owner and that cross-tenant reads and inserts fail.
- Creating a tenant needs one special step: the transaction is bound to the new tenant before the owner membership is inserted.
- Postgres reports RLS violations as SQLSTATE `42501`, which Spring wraps as a "bad SQL grammar" exception, so tests assert on the root cause.
