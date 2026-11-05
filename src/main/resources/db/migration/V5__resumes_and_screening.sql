CREATE TABLE resumes (
    id              UUID                     PRIMARY KEY,
    tenant_id       UUID                     NOT NULL REFERENCES tenants (id),
    job_id          UUID                     NOT NULL REFERENCES jobs (id) ON DELETE CASCADE,
    file_name       TEXT                     NOT NULL,
    content_type    TEXT                     NOT NULL,
    storage_key     TEXT                     NOT NULL,
    content_hash    TEXT                     NOT NULL,
    text            TEXT                     NOT NULL,
    github_username TEXT,
    uploaded_by     TEXT                     NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    UNIQUE (job_id, content_hash)
);

CREATE TABLE screening_results (
    id             UUID                     PRIMARY KEY,
    tenant_id      UUID                     NOT NULL REFERENCES tenants (id),
    resume_id      UUID                     NOT NULL UNIQUE REFERENCES resumes (id) ON DELETE CASCADE,
    job_id         UUID                     NOT NULL REFERENCES jobs (id) ON DELETE CASCADE,
    file_name      TEXT                     NOT NULL,
    status         TEXT                     NOT NULL,
    keyword_score  INT                      NOT NULL,
    ai_score       INT,
    final_score    INT                      NOT NULL,
    recommendation TEXT,
    report         JSONB                    NOT NULL,
    error          TEXT,
    model          TEXT,
    tokens_in      INT                      NOT NULL DEFAULT 0,
    tokens_out     INT                      NOT NULL DEFAULT 0,
    screened_at    TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX screening_results_job_score_idx ON screening_results (job_id, final_score DESC);

ALTER TABLE resumes ENABLE ROW LEVEL SECURITY;
ALTER TABLE screening_results ENABLE ROW LEVEL SECURITY;

CREATE POLICY resumes_tenant ON resumes
    FOR ALL
    USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid);

CREATE POLICY screening_results_tenant ON screening_results
    FOR ALL
    USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid);
