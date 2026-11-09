ALTER TABLE screening_results
    ADD COLUMN override_score  INT,
    ADD COLUMN override_reason TEXT,
    ADD COLUMN overridden_by   TEXT,
    ADD COLUMN overridden_at   TIMESTAMP WITH TIME ZONE,
    ADD COLUMN rank_score      INT;

UPDATE screening_results SET rank_score = final_score;

ALTER TABLE screening_results ALTER COLUMN rank_score SET NOT NULL;

DROP INDEX screening_results_job_score_idx;

CREATE INDEX screening_results_job_rank_idx ON screening_results (job_id, rank_score DESC);

CREATE TABLE score_overrides (
    id             UUID                     PRIMARY KEY,
    tenant_id      UUID                     NOT NULL REFERENCES tenants (id),
    result_id      UUID                     NOT NULL REFERENCES screening_results (id) ON DELETE CASCADE,
    previous_score INT                      NOT NULL,
    new_score      INT,
    reason         TEXT,
    reviewer_email TEXT                     NOT NULL,
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX score_overrides_result_idx ON score_overrides (result_id, created_at DESC);

ALTER TABLE score_overrides ENABLE ROW LEVEL SECURITY;

CREATE POLICY score_overrides_tenant ON score_overrides
    FOR ALL
    USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid);
