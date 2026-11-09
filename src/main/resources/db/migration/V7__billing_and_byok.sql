CREATE TABLE subscriptions (
    id                UUID                     PRIMARY KEY,
    tenant_id         UUID                     NOT NULL UNIQUE REFERENCES tenants (id),
    plan              TEXT                     NOT NULL,
    period_start      TIMESTAMP WITH TIME ZONE NOT NULL,
    period_end        TIMESTAMP WITH TIME ZONE NOT NULL,
    credits_remaining INT
);

CREATE TABLE payments (
    id           UUID                     PRIMARY KEY,
    tenant_id    UUID                     NOT NULL REFERENCES tenants (id),
    plan         TEXT                     NOT NULL,
    amount_usd   INT                      NOT NULL,
    status       TEXT                     NOT NULL,
    provider     TEXT                     NOT NULL,
    created_by   TEXT                     NOT NULL,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL,
    completed_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE credit_ledger (
    id         UUID                     PRIMARY KEY,
    tenant_id  UUID                     NOT NULL REFERENCES tenants (id),
    type       TEXT                     NOT NULL,
    amount     INT                      NOT NULL,
    batch_id   UUID,
    resume_id  UUID,
    note       TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX credit_ledger_tenant_idx ON credit_ledger (tenant_id, created_at DESC);

CREATE TABLE tenant_llm_settings (
    tenant_id         UUID                     PRIMARY KEY REFERENCES tenants (id),
    base_url          TEXT                     NOT NULL,
    model             TEXT                     NOT NULL,
    api_key_encrypted TEXT                     NOT NULL,
    api_key_last4     TEXT                     NOT NULL,
    updated_at        TIMESTAMP WITH TIME ZONE NOT NULL
);

INSERT INTO subscriptions (id, tenant_id, plan, period_start, period_end, credits_remaining)
SELECT gen_random_uuid(), id, 'FREE', now(), now() + INTERVAL '30 days', 50
FROM tenants;

INSERT INTO credit_ledger (id, tenant_id, type, amount, note, created_at)
SELECT gen_random_uuid(), id, 'GRANT', 50, 'FREE plan', now()
FROM tenants;

ALTER TABLE subscriptions ENABLE ROW LEVEL SECURITY;
ALTER TABLE payments ENABLE ROW LEVEL SECURITY;
ALTER TABLE credit_ledger ENABLE ROW LEVEL SECURITY;
ALTER TABLE tenant_llm_settings ENABLE ROW LEVEL SECURITY;

CREATE POLICY subscriptions_tenant ON subscriptions
    FOR ALL
    USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid);

CREATE POLICY payments_tenant ON payments
    FOR ALL
    USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid);

CREATE POLICY credit_ledger_tenant ON credit_ledger
    FOR ALL
    USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid);

CREATE POLICY tenant_llm_settings_tenant ON tenant_llm_settings
    FOR ALL
    USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid);
