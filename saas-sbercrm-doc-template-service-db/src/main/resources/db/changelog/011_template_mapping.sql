-- liquibase formatted sql

-- changeset codex:011_template_mapping
CREATE TABLE IF NOT EXISTS t_template_mapping (
  id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id    UUID NOT NULL,

  template_id  UUID NOT NULL,

  key          VARCHAR(128) NOT NULL,
  definition   JSONB NOT NULL,

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

-- changeset codex:011_template_mapping_comments runOnChange:true
COMMENT ON TABLE t_template_mapping IS
'Маппинги переменных шаблона. Хранит ключ переменной и JSON-описание способа получения значения. Для пользовательских операций применяется RLS по tenant_id.';

COMMENT ON COLUMN t_template_mapping.id IS 'Идентификатор маппинга переменной.';
COMMENT ON COLUMN t_template_mapping.tenant_id IS 'Идентификатор тенанта для изоляции данных через RLS.';
COMMENT ON COLUMN t_template_mapping.template_id IS 'Идентификатор шаблона, которому принадлежит маппинг.';
COMMENT ON COLUMN t_template_mapping.key IS 'Ключ переменной, используемый в placeholder шаблона. Уникален в пределах шаблона.';
COMMENT ON COLUMN t_template_mapping.definition IS 'JSON-описание маппинга переменной, содержащее scope и value.';
COMMENT ON COLUMN t_template_mapping.created_by IS 'Идентификатор пользователя, создавшего маппинг.';
COMMENT ON COLUMN t_template_mapping.updated_by IS 'Идентификатор пользователя, последним обновившего маппинг.';
COMMENT ON COLUMN t_template_mapping.created_at IS 'Дата и время создания маппинга.';
COMMENT ON COLUMN t_template_mapping.updated_at IS 'Дата и время последнего обновления маппинга.';
