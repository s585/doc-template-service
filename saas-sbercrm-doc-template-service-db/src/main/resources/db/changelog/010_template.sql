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
'Метаданные шаблона. В рамках MVP хранится только одно актуальное состояние без версионирования. Для пользовательских операций применяется RLS по tenant_id.';

COMMENT ON COLUMN t_template.id IS 'Идентификатор шаблона.';
COMMENT ON COLUMN t_template.tenant_id IS 'Идентификатор тенанта для изоляции данных через RLS.';
COMMENT ON COLUMN t_template.entity_id IS 'Идентификатор бизнес-сущности, к которой относится шаблон.';
COMMENT ON COLUMN t_template.name IS 'Отображаемое название шаблона.';
COMMENT ON COLUMN t_template.system_name IS 'Системный код шаблона.';
COMMENT ON COLUMN t_template.format IS 'Формат бинарного файла шаблона.';
COMMENT ON COLUMN t_template.s3_key IS 'Ключ файла шаблона в S3.';
COMMENT ON COLUMN t_template.status IS 'Статус шаблона.';
COMMENT ON COLUMN t_template.created_by IS 'Идентификатор пользователя, создавшего шаблон.';
COMMENT ON COLUMN t_template.updated_by IS 'Идентификатор пользователя, последним обновившего шаблон.';
COMMENT ON COLUMN t_template.created_at IS 'Дата и время создания шаблона.';
COMMENT ON COLUMN t_template.updated_at IS 'Дата и время последнего обновления шаблона.';
