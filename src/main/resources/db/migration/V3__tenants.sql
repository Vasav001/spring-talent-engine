CREATE TABLE tenants (
    id         UUID                     PRIMARY KEY,
    name       TEXT                     NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE tenant_members (
    id         UUID                     PRIMARY KEY,
    tenant_id  UUID                     NOT NULL REFERENCES tenants (id),
    email      TEXT                     NOT NULL,
    role       TEXT                     NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    UNIQUE (tenant_id, email)
);

CREATE INDEX tenant_members_email_idx ON tenant_members (email);

ALTER TABLE tenant_members ENABLE ROW LEVEL SECURITY;

CREATE POLICY tenant_members_read_own ON tenant_members
    FOR SELECT
    USING (email = current_setting('app.user_email', true));

CREATE POLICY tenant_members_tenant ON tenant_members
    FOR ALL
    USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid);
