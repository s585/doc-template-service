-- liquibase formatted sql

-- changeset codex:030_generation_job
CREATE TABLE IF NOT EXISTS t_generation_job (
  id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),

  tenant_id         UUID NOT NULL,

  document_id       UUID NOT NULL,

  template_id       UUID NOT NULL,
  entity_id         UUID NOT NULL,
  object_id         UUID NOT NULL,

  requested_formats VARCHAR(16)[] NOT NULL,
  status            VARCHAR(32) NOT NULL,

  locked_by         UUID,
  locked_until      TIMESTAMPTZ,

  created_by        UUID,
  updated_by        UUID,
  created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),

  error_code        VARCHAR(64),
  error_message     VARCHAR(1024)
);

CREATE INDEX IF NOT EXISTS idx_t_generation_job_status
  ON t_generation_job (status, created_at);

CREATE INDEX IF NOT EXISTS idx_t_generation_job_lock
  ON t_generation_job (locked_until);

CREATE INDEX IF NOT EXISTS idx_t_generation_job_document
  ON t_generation_job (document_id);

CREATE INDEX IF NOT EXISTS idx_t_generation_job_tenant
  ON t_generation_job (tenant_id);

ALTER TABLE t_generation_job DISABLE ROW LEVEL SECURITY;

-- changeset codex:030_generation_job_comments runOnChange:true
COMMENT ON TABLE t_generation_job IS
'Internal orchestration queue. NOT used for UI/read APIs. tenant_id present but RLS is DISABLED on this table.';

COMMENT ON COLUMN t_generation_job.locked_until IS
'Lease lock: if worker dies, another worker may pick the job after this timestamp.';
COMMENT ON COLUMN t_generation_job.requested_formats IS
'Formats to generate for the document (e.g., DOCX/XLSX primary + optional PDF).';
