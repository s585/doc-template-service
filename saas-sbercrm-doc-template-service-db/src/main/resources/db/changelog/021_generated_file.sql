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
'Сгенерированные файлы, относящиеся к generated_document. Статус отслеживается отдельно для каждого файла. Для пользовательских операций применяется RLS по tenant_id.';

COMMENT ON COLUMN t_generated_file.id IS 'Идентификатор сгенерированного файла.';
COMMENT ON COLUMN t_generated_file.tenant_id IS 'Идентификатор тенанта для изоляции данных через RLS.';
COMMENT ON COLUMN t_generated_file.document_id IS 'Идентификатор generated_document, к которому относится файл.';
COMMENT ON COLUMN t_generated_file.format IS 'Формат сгенерированного файла.';
COMMENT ON COLUMN t_generated_file.status IS 'Статус жизненного цикла файла. Позволяет отслеживать состояние каждого формата отдельно.';
COMMENT ON COLUMN t_generated_file.s3_key IS 'Ключ файла в S3. Заполняется после успешной генерации.';
COMMENT ON COLUMN t_generated_file.checksum IS 'Контрольная сумма сгенерированного файла.';
COMMENT ON COLUMN t_generated_file.size_bytes IS 'Размер файла в байтах.';
COMMENT ON COLUMN t_generated_file.error_code IS 'Код ошибки генерации файла.';
COMMENT ON COLUMN t_generated_file.error_message IS 'Текстовое описание ошибки генерации файла.';
COMMENT ON COLUMN t_generated_file.created_by IS 'Идентификатор пользователя, создавшего запись о файле.';
COMMENT ON COLUMN t_generated_file.updated_by IS 'Идентификатор пользователя, последним обновившего запись о файле.';
COMMENT ON COLUMN t_generated_file.created_at IS 'Дата и время создания записи о файле.';
COMMENT ON COLUMN t_generated_file.updated_at IS 'Дата и время последнего обновления записи о файле.';
