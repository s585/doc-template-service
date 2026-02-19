-- liquibase formatted sql

-- changeset codex:031_generation_job_attempt
CREATE TABLE IF NOT EXISTS t_generation_job_attempt (
  id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  job_id        UUID NOT NULL,
  attempt_no    INT NOT NULL,

  worker_id     UUID,
  started_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
  finished_at   TIMESTAMPTZ,

  status        VARCHAR(32) NOT NULL,
  error_code    VARCHAR(64),
  error_message VARCHAR(1024),

  created_by    UUID,
  updated_by    UUID
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_t_generation_job_attempt_no
  ON t_generation_job_attempt (job_id, attempt_no);

CREATE INDEX IF NOT EXISTS idx_t_generation_job_attempt_job
  ON t_generation_job_attempt (job_id, started_at desc);

ALTER TABLE t_generation_job_attempt DISABLE ROW LEVEL SECURITY;

-- changeset codex:031_generation_job_attempt_comments runOnChange:true
COMMENT ON TABLE t_generation_job_attempt IS
'Internal job attempts (debug/audit). Not used by UI. RLS disabled.';
