-- liquibase formatted sql

-- changeset codex:012_template_condition
CREATE TABLE IF NOT EXISTS t_template_condition (
  id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id      UUID NOT NULL,

  template_id    UUID NOT NULL,
  condition      JSONB NOT NULL,

  created_by     UUID,
  updated_by     UUID,
  created_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_t_template_condition_tenant_template
  ON t_template_condition (tenant_id, template_id);

ALTER TABLE t_template_condition ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS rls_t_template_condition_tenant ON t_template_condition;
CREATE POLICY rls_t_template_condition_tenant
ON t_template_condition
USING (tenant_id = nullif(current_setting('app.tenant_id', true), '')::uuid);

-- changeset codex:012_template_condition_comments runOnChange:true
COMMENT ON TABLE t_template_condition IS
'Availability condition for templates (AND/OR up to configured depth). User-facing: RLS by tenant_id.';
