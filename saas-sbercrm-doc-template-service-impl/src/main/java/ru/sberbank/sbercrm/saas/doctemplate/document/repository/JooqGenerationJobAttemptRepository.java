package ru.sberbank.sbercrm.saas.doctemplate.document.repository;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;
import ru.sberbank.sbercrm.saas.doctemplate.document.converter.GenerationArtifactMetaRecordConverter;
import ru.sberbank.sbercrm.saas.doctemplate.document.converter.GenerationJobAttemptRecordConverter;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.GenerationArtifactMeta;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.GenerationJobAttempt;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.GenerationJobAttemptStatus;

import static ru.sberbank.sbercrm.saas.doctemplate.jooq.tables.TGenerationJobAttempt.T_GENERATION_JOB_ATTEMPT;

@Repository
@RequiredArgsConstructor
public class JooqGenerationJobAttemptRepository implements GenerationJobAttemptRepository {
    private final DSLContext dslContext;
    private final Clock clock;
    private final GenerationArtifactMetaRecordConverter generationArtifactMetaRecordConverter;
    private final GenerationJobAttemptRecordConverter generationJobAttemptRecordConverter;

    @Override
    public GenerationJobAttempt create(UUID userId, UUID jobId, UUID workerId) {
        OffsetDateTime now = OffsetDateTime.now(clock);
        return dslContext.insertInto(T_GENERATION_JOB_ATTEMPT)
            .set(T_GENERATION_JOB_ATTEMPT.JOB_ID, jobId)
            .set(
                T_GENERATION_JOB_ATTEMPT.ATTEMPT_NO,
                DSL.select(DSL.coalesce(DSL.max(T_GENERATION_JOB_ATTEMPT.ATTEMPT_NO), 0).add(1))
                    .from(T_GENERATION_JOB_ATTEMPT)
                    .where(T_GENERATION_JOB_ATTEMPT.JOB_ID.eq(jobId))
            )
            .set(T_GENERATION_JOB_ATTEMPT.WORKER_ID, workerId)
            .set(T_GENERATION_JOB_ATTEMPT.STARTED_AT, now)
            .set(T_GENERATION_JOB_ATTEMPT.STATUS, GenerationJobAttemptStatus.PROCESSING.name())
            .set(T_GENERATION_JOB_ATTEMPT.CREATED_BY, userId)
            .set(T_GENERATION_JOB_ATTEMPT.UPDATED_BY, userId)
            .returning()
            .fetchOne(generationJobAttemptRecordConverter);
    }

    @Override
    public List<GenerationJobAttempt> findByJobId(UUID jobId) {
        return dslContext.selectFrom(T_GENERATION_JOB_ATTEMPT)
            .where(T_GENERATION_JOB_ATTEMPT.JOB_ID.eq(jobId))
            .orderBy(T_GENERATION_JOB_ATTEMPT.ATTEMPT_NO.asc())
            .fetch(generationJobAttemptRecordConverter);
    }

    @Override
    public void markArtifactUploaded(UUID userId, UUID attemptId, GenerationArtifactMeta artifactMeta) {
        dslContext.update(T_GENERATION_JOB_ATTEMPT)
            .set(T_GENERATION_JOB_ATTEMPT.STATUS, GenerationJobAttemptStatus.UPLOADED.name())
            .set(T_GENERATION_JOB_ATTEMPT.FINISHED_AT, (OffsetDateTime) null)
            .set(T_GENERATION_JOB_ATTEMPT.ARTIFACT_S3_KEY, artifactMeta.s3Key())
            .set(T_GENERATION_JOB_ATTEMPT.ARTIFACT_CHECKSUM, artifactMeta.checksum())
            .set(T_GENERATION_JOB_ATTEMPT.ARTIFACT_SIZE_BYTES, artifactMeta.sizeBytes())
            .set(T_GENERATION_JOB_ATTEMPT.ERROR_CODE, (String) null)
            .set(T_GENERATION_JOB_ATTEMPT.ERROR_MESSAGE, (String) null)
            .set(T_GENERATION_JOB_ATTEMPT.UPDATED_BY, userId)
            .where(T_GENERATION_JOB_ATTEMPT.ID.eq(attemptId))
            .execute();
    }

    @Override
    public void markCompleted(UUID userId, UUID attemptId, GenerationArtifactMeta artifactMeta) {
        OffsetDateTime now = OffsetDateTime.now(clock);
        dslContext.update(T_GENERATION_JOB_ATTEMPT)
            .set(T_GENERATION_JOB_ATTEMPT.STATUS, GenerationJobAttemptStatus.DONE.name())
            .set(T_GENERATION_JOB_ATTEMPT.FINISHED_AT, now)
            .set(T_GENERATION_JOB_ATTEMPT.ARTIFACT_S3_KEY, artifactMeta.s3Key())
            .set(T_GENERATION_JOB_ATTEMPT.ARTIFACT_CHECKSUM, artifactMeta.checksum())
            .set(T_GENERATION_JOB_ATTEMPT.ARTIFACT_SIZE_BYTES, artifactMeta.sizeBytes())
            .set(T_GENERATION_JOB_ATTEMPT.ERROR_CODE, (String) null)
            .set(T_GENERATION_JOB_ATTEMPT.ERROR_MESSAGE, (String) null)
            .set(T_GENERATION_JOB_ATTEMPT.UPDATED_BY, userId)
            .where(T_GENERATION_JOB_ATTEMPT.ID.eq(attemptId))
            .execute();
    }

    @Override
    public void markFailed(UUID userId, UUID attemptId, String errorCode, String errorMessage) {
        OffsetDateTime now = OffsetDateTime.now(clock);
        dslContext.update(T_GENERATION_JOB_ATTEMPT)
            .set(T_GENERATION_JOB_ATTEMPT.STATUS, GenerationJobAttemptStatus.ERROR.name())
            .set(T_GENERATION_JOB_ATTEMPT.FINISHED_AT, now)
            .set(T_GENERATION_JOB_ATTEMPT.ERROR_CODE, errorCode)
            .set(T_GENERATION_JOB_ATTEMPT.ERROR_MESSAGE, errorMessage)
            .set(T_GENERATION_JOB_ATTEMPT.UPDATED_BY, userId)
            .where(T_GENERATION_JOB_ATTEMPT.ID.eq(attemptId))
            .execute();
    }

    @Override
    public void markTimedOutActiveAttempt(UUID userId, UUID jobId) {
        UUID attemptId = dslContext.select(T_GENERATION_JOB_ATTEMPT.ID)
            .from(T_GENERATION_JOB_ATTEMPT)
            .where(
                T_GENERATION_JOB_ATTEMPT.JOB_ID.eq(jobId),
                T_GENERATION_JOB_ATTEMPT.FINISHED_AT.isNull()
            )
            .orderBy(T_GENERATION_JOB_ATTEMPT.ATTEMPT_NO.desc())
            .limit(1)
            .fetchOne(T_GENERATION_JOB_ATTEMPT.ID);

        if (attemptId == null) {
            return;
        }

        OffsetDateTime now = OffsetDateTime.now(clock);
        dslContext.update(T_GENERATION_JOB_ATTEMPT)
            .set(T_GENERATION_JOB_ATTEMPT.STATUS, GenerationJobAttemptStatus.TIMEOUT.name())
            .set(T_GENERATION_JOB_ATTEMPT.FINISHED_AT, now)
            .set(T_GENERATION_JOB_ATTEMPT.ERROR_CODE, "generation.job_timeout")
            .set(T_GENERATION_JOB_ATTEMPT.ERROR_MESSAGE, "Generation job timed out")
            .set(T_GENERATION_JOB_ATTEMPT.UPDATED_BY, userId)
            .where(T_GENERATION_JOB_ATTEMPT.ID.eq(attemptId))
            .execute();
    }

    @Override
    public Optional<GenerationArtifactMeta> findLatestArtifactBeforeAttempt(UUID jobId, int attemptNo) {
        if (attemptNo <= 1) {
            return Optional.empty();
        }
        GenerationArtifactMeta artifact = dslContext.select(
                T_GENERATION_JOB_ATTEMPT.ARTIFACT_S3_KEY,
                T_GENERATION_JOB_ATTEMPT.ARTIFACT_CHECKSUM,
                T_GENERATION_JOB_ATTEMPT.ARTIFACT_SIZE_BYTES
            )
            .from(T_GENERATION_JOB_ATTEMPT)
            .where(
                T_GENERATION_JOB_ATTEMPT.JOB_ID.eq(jobId),
                T_GENERATION_JOB_ATTEMPT.ATTEMPT_NO.lt(attemptNo),
                T_GENERATION_JOB_ATTEMPT.STATUS.in(
                    GenerationJobAttemptStatus.DONE.name(),
                    GenerationJobAttemptStatus.UPLOADED.name()
                ),
                T_GENERATION_JOB_ATTEMPT.ARTIFACT_S3_KEY.isNotNull(),
                T_GENERATION_JOB_ATTEMPT.ARTIFACT_S3_KEY.ne("")
            )
            .orderBy(T_GENERATION_JOB_ATTEMPT.ATTEMPT_NO.desc())
            .limit(1)
            .fetchOne(generationArtifactMetaRecordConverter);
        if (artifact == null) {
            return Optional.empty();
        }
        return Optional.of(artifact);
    }
}
