-- liquibase formatted sql

-- changeset codex:020_generated_document
CREATE TABLE IF NOT EXISTS t_generated_document (
  id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id        UUID NOT NULL,

  template_id      UUID NOT NULL,

  entity_id        UUID NOT NULL,
  object_id        UUID NOT NULL,

  request_id       UUID NOT NULL,

  created_by       UUID,
  updated_by       UUID,
  created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_t_generated_document_tenant_entity
  ON t_generated_document (tenant_id, entity_id, object_id);

CREATE INDEX IF NOT EXISTS idx_t_generated_document_created_at
  ON t_generated_document (created_at);

ALTER TABLE t_generated_document ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS rls_t_generated_document_tenant ON t_generated_document;
CREATE POLICY rls_t_generated_document_tenant
ON t_generated_document
USING (tenant_id = nullif(current_setting('app.tenant_id', true), '')::uuid);

-- changeset codex:020_generated_document_comments runOnChange:true
COMMENT ON TABLE t_generated_document IS
'Сущность read-model для UI, отражающая один пользовательский запрос на генерацию и его результат. Это не очередь заданий. Для пользовательских операций применяется RLS по tenant_id.';

COMMENT ON COLUMN t_generated_document.id IS 'Идентификатор сгенерированного документа.';
COMMENT ON COLUMN t_generated_document.tenant_id IS 'Идентификатор тенанта для изоляции данных через RLS.';
COMMENT ON COLUMN t_generated_document.template_id IS 'Идентификатор шаблона, по которому выполнялась генерация.';
COMMENT ON COLUMN t_generated_document.entity_id IS 'Идентификатор сущности исходного объекта.';
COMMENT ON COLUMN t_generated_document.object_id IS 'Идентификатор исходного объекта, по которому выполнялась генерация.';
COMMENT ON COLUMN t_generated_document.request_id IS 'Идемпотентный идентификатор запроса на генерацию, уникальный в пределах тенанта.';
COMMENT ON COLUMN t_generated_document.created_by IS 'Идентификатор пользователя, инициировавшего генерацию.';
COMMENT ON COLUMN t_generated_document.updated_by IS 'Идентификатор пользователя, последним обновившего запись.';
COMMENT ON COLUMN t_generated_document.created_at IS 'Дата и время создания записи о генерации.';
COMMENT ON COLUMN t_generated_document.updated_at IS 'Дата и время последнего обновления записи о генерации.';
