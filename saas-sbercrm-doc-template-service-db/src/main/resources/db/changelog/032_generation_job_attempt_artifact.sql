-- liquibase formatted sql

-- changeset codex:032_generation_job_attempt_artifact
ALTER TABLE t_generation_job_attempt
    ADD COLUMN IF NOT EXISTS artifact_s3_key VARCHAR(1024);

ALTER TABLE t_generation_job_attempt
    ADD COLUMN IF NOT EXISTS artifact_checksum VARCHAR(128);

ALTER TABLE t_generation_job_attempt
    ADD COLUMN IF NOT EXISTS artifact_size_bytes BIGINT;

CREATE INDEX IF NOT EXISTS idx_t_generation_job_attempt_artifact_key
    ON t_generation_job_attempt (job_id, attempt_no DESC)
    WHERE artifact_s3_key IS NOT NULL;

-- changeset codex:032_generation_job_attempt_artifact_comments runOnChange:true
COMMENT ON COLUMN t_generation_job_attempt.artifact_s3_key IS
'Ключ артефакта в file-storage, созданного или переиспользованного при успешном завершении attempt.';

COMMENT ON COLUMN t_generation_job_attempt.artifact_checksum IS
'SHA-256 контрольная сумма артефакта в file-storage.';

COMMENT ON COLUMN t_generation_job_attempt.artifact_size_bytes IS
'Размер артефакта в байтах в file-storage.';
