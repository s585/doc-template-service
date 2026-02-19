-- liquibase formatted sql

-- changeset codex:010_template
CREATE TABLE IF NOT EXISTS t_template (
  id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id    UUID NOT NULL,

  entity_id    UUID NOT NULL,
  name         VARCHAR(255) NOT NULL,
  system_name  VARCHAR(255) NOT NULL,
  format       VARCHAR(32) NOT NULL,
  s3_key       TEXT NOT NULL,

  status       VARCHAR(32) NOT NULL,

  created_by   UUID,
  updated_by   UUID,
  created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_t_template_tenant_entity
  ON t_template (tenant_id, entity_id);

ALTER TABLE t_template ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS rls_t_template_tenant ON t_template;
CREATE POLICY rls_t_template_tenant
ON t_template
USING (tenant_id = nullif(current_setting('app.tenant_id', true), '')::uuid);

-- changeset codex:010_template_comments runOnChange:true
COMMENT ON TABLE t_template IS
'Template metadata. MVP: single current state (no versioning). User-facing: RLS by tenant_id.';

COMMENT ON COLUMN t_template.tenant_id IS 'Tenant identifier for RLS isolation.';
COMMENT ON COLUMN t_template.entity_id IS 'Identifier of business entity type this template belongs to.';
COMMENT ON COLUMN t_template.s3_key IS 'S3 key of template binary (DOCX/XLSX).';
