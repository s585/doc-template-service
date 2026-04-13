-- liquibase formatted sql

-- changeset codex:030_generation_job
CREATE TABLE IF NOT EXISTS t_generation_job (
  id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),

  tenant_id         UUID NOT NULL,

  document_id       UUID NOT NULL,

  template_id       UUID NOT NULL,
  entity_id         UUID NOT NULL,
  object_id         UUID NOT NULL,

  format            VARCHAR(16) NOT NULL,
  status            VARCHAR(32) NOT NULL,
  attempt_count     INT NOT NULL DEFAULT 0,
  next_retry_at     TIMESTAMPTZ,

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
  ON t_generation_job (status, next_retry_at, created_at);

CREATE INDEX IF NOT EXISTS idx_t_generation_job_lock
  ON t_generation_job (locked_until);

CREATE INDEX IF NOT EXISTS idx_t_generation_job_document
  ON t_generation_job (document_id);

CREATE INDEX IF NOT EXISTS idx_t_generation_job_document_format
  ON t_generation_job (document_id, format);

CREATE INDEX IF NOT EXISTS idx_t_generation_job_tenant
  ON t_generation_job (tenant_id);

ALTER TABLE t_generation_job DISABLE ROW LEVEL SECURITY;

-- changeset codex:030_generation_job_comments runOnChange:true
COMMENT ON TABLE t_generation_job IS
'Внутренняя очередь оркестрации генерации. Не используется напрямую в UI и read API. Поле tenant_id присутствует, но RLS для таблицы отключен.';

COMMENT ON COLUMN t_generation_job.id IS 'Идентификатор задания на генерацию.';
COMMENT ON COLUMN t_generation_job.tenant_id IS 'Идентификатор тенанта, к которому относится задание.';
COMMENT ON COLUMN t_generation_job.document_id IS 'Идентификатор generated_document, для которого создается задание.';
COMMENT ON COLUMN t_generation_job.template_id IS 'Идентификатор шаблона, используемого для генерации.';
COMMENT ON COLUMN t_generation_job.entity_id IS 'Идентификатор сущности исходного объекта.';
COMMENT ON COLUMN t_generation_job.object_id IS 'Идентификатор исходного объекта, по которому выполняется генерация.';
COMMENT ON COLUMN t_generation_job.format IS 'Формат, для которого создано конкретное задание на генерацию. Одно задание соответствует одному формату.';
COMMENT ON COLUMN t_generation_job.status IS 'Текущий статус задания на генерацию.';
COMMENT ON COLUMN t_generation_job.attempt_count IS 'Количество завершенных попыток выполнения задания.';
COMMENT ON COLUMN t_generation_job.next_retry_at IS 'Момент времени, после которого допускается повторная попытка выполнения задания.';
COMMENT ON COLUMN t_generation_job.locked_by IS 'Идентификатор воркера, который в данный момент удерживает задание.';
COMMENT ON COLUMN t_generation_job.locked_until IS 'Время окончания lease-блокировки. После него другое приложение может забрать задание.';
COMMENT ON COLUMN t_generation_job.created_by IS 'Идентификатор пользователя, создавшего задание.';
COMMENT ON COLUMN t_generation_job.updated_by IS 'Идентификатор пользователя или процесса, последним обновившего задание.';
COMMENT ON COLUMN t_generation_job.created_at IS 'Дата и время создания задания.';
COMMENT ON COLUMN t_generation_job.updated_at IS 'Дата и время последнего обновления задания.';
COMMENT ON COLUMN t_generation_job.error_code IS 'Код последней ошибки при обработке задания.';
COMMENT ON COLUMN t_generation_job.error_message IS 'Текстовое описание последней ошибки при обработке задания.';
