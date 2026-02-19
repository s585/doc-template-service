-- liquibase formatted sql

-- changeset codex:011_template_variable
CREATE TABLE IF NOT EXISTS t_template_mapping (
  id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id    UUID NOT NULL,

  template_id  UUID NOT NULL,

  code         VARCHAR(128) NOT NULL,
  expression   JSONB NOT NULL,
  value_type   VARCHAR(64) NOT NULL,

  created_by   UUID,
  updated_by   UUID,
  created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_t_template_mapping_tenant_template
  ON t_template_mapping (tenant_id, template_id);

ALTER TABLE t_template_mapping ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS rls_t_template_mapping_tenant ON t_template_mapping;
CREATE POLICY rls_t_template_mapping_tenant
ON t_template_mapping
USING (tenant_id = nullif(current_setting('app.tenant_id', true), '')::uuid);

-- changeset codex:011_template_variable_comments runOnChange:true
COMMENT ON TABLE t_template_mapping IS
'Template variables. Stores expressions used to compute values. User-facing: RLS by tenant_id.';

COMMENT ON COLUMN t_template_mapping.code IS
'Variable key used in template placeholders. Unique per template.';
COMMENT ON COLUMN t_template_mapping.expression IS
'Expression (EL) to resolve variable value. Evaluated during generation.';
