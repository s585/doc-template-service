-- liquibase formatted sql

-- changeset codex:021_generated_file
CREATE TABLE IF NOT EXISTS t_generated_file (
  id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id       UUID NOT NULL,

  document_id     UUID NOT NULL,

  format          VARCHAR(32) NOT NULL,
  status          VARCHAR(32) NOT NULL,

  s3_key          TEXT,
  checksum        TEXT,
  size_bytes      BIGINT,

  error_code      VARCHAR(64),
  error_message   VARCHAR(1024),

  created_by      UUID,
  updated_by      UUID,
  created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_t_generated_file_document_format
  ON t_generated_file (document_id, format);

CREATE INDEX IF NOT EXISTS idx_t_generated_file_status
  ON t_generated_file (status);

ALTER TABLE t_generated_file ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS rls_t_generated_file_tenant ON t_generated_file;
CREATE POLICY rls_t_generated_file_tenant
ON t_generated_file
USING (tenant_id = nullif(current_setting('app.tenant_id', true), '')::uuid);

-- changeset codex:021_generated_file_comments runOnChange:true
COMMENT ON TABLE t_generated_file IS
'Generated artifacts (files) for a generated_document. Status tracked per file. RLS by tenant_id.';

COMMENT ON COLUMN t_generated_file.s3_key IS
'S3 key of generated artifact. Filled when status=DONE.';
COMMENT ON COLUMN t_generated_file.status IS
'Per-file lifecycle status. Allows DOCX DONE while PDF IN_PROGRESS, etc.';
