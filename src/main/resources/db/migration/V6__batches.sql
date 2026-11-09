CREATE TABLE batches (
    id           UUID                     PRIMARY KEY,
    tenant_id    UUID                     NOT NULL REFERENCES tenants (id),
    job_id       UUID                     NOT NULL REFERENCES jobs (id) ON DELETE CASCADE,
    status       TEXT                     NOT NULL,
    total        INT                      NOT NULL,
    processed    INT                      NOT NULL DEFAULT 0,
    failed       INT                      NOT NULL DEFAULT 0,
    created_by   TEXT                     NOT NULL,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL,
    completed_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX batches_job_idx ON batches (job_id);

ALTER TABLE resumes
    ADD COLUMN batch_id UUID REFERENCES batches (id) ON DELETE CASCADE,
    ADD COLUMN status   TEXT NOT NULL DEFAULT 'DONE',
    ADD COLUMN error    TEXT,
    ALTER COLUMN content_hash DROP NOT NULL,
    ALTER COLUMN text DROP NOT NULL;

ALTER TABLE resumes ALTER COLUMN status DROP DEFAULT;

CREATE INDEX resumes_batch_idx ON resumes (batch_id);

ALTER TABLE batches ENABLE ROW LEVEL SECURITY;

CREATE POLICY batches_tenant ON batches
    FOR ALL
    USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid);
