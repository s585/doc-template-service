-- liquibase formatted sql

-- changeset codex:031_generation_job_attempt
CREATE TABLE IF NOT EXISTS t_generation_job_attempt (
  id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  job_id        UUID NOT NULL,
  attempt_no    INT NOT NULL,

  worker_id     UUID,
  started_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
  finished_at   TIMESTAMPTZ,

  status        VARCHAR(32) NOT NULL,
  error_code    VARCHAR(64),
  error_message VARCHAR(1024),

  created_by    UUID,
  updated_by    UUID
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_t_generation_job_attempt_no
  ON t_generation_job_attempt (job_id, attempt_no);

CREATE INDEX IF NOT EXISTS idx_t_generation_job_attempt_job
  ON t_generation_job_attempt (job_id, started_at desc);

ALTER TABLE t_generation_job_attempt DISABLE ROW LEVEL SECURITY;

-- changeset codex:031_generation_job_attempt_comments runOnChange:true
COMMENT ON TABLE t_generation_job_attempt IS
'Попытки выполнения внутреннего задания на генерацию. Используются для отладки и аудита, не предназначены для UI. RLS отключен.';

COMMENT ON COLUMN t_generation_job_attempt.id IS 'Идентификатор попытки выполнения задания.';
COMMENT ON COLUMN t_generation_job_attempt.job_id IS 'Идентификатор задания на генерацию.';
COMMENT ON COLUMN t_generation_job_attempt.attempt_no IS 'Порядковый номер попытки выполнения задания.';
COMMENT ON COLUMN t_generation_job_attempt.worker_id IS 'Идентификатор воркера, выполнявшего попытку.';
COMMENT ON COLUMN t_generation_job_attempt.started_at IS 'Дата и время начала попытки.';
COMMENT ON COLUMN t_generation_job_attempt.finished_at IS 'Дата и время завершения попытки.';
COMMENT ON COLUMN t_generation_job_attempt.status IS 'Статус конкретной попытки выполнения.';
COMMENT ON COLUMN t_generation_job_attempt.error_code IS 'Код ошибки, возникшей в рамках попытки.';
COMMENT ON COLUMN t_generation_job_attempt.error_message IS 'Текстовое описание ошибки, возникшей в рамках попытки.';
COMMENT ON COLUMN t_generation_job_attempt.created_by IS 'Идентификатор пользователя или процесса, создавшего запись о попытке.';
COMMENT ON COLUMN t_generation_job_attempt.updated_by IS 'Идентификатор пользователя или процесса, последним обновившего запись о попытке.';
