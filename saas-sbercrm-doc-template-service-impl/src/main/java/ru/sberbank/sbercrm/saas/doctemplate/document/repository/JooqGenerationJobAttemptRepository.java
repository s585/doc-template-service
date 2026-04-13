package ru.sberbank.sbercrm.saas.doctemplate.document.repository;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;
import ru.sberbank.sbercrm.saas.doctemplate.document.constant.DocumentConstants;
import ru.sberbank.sbercrm.saas.doctemplate.document.converter.GenerationJobAttemptRecordConverter;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.GenerationJobAttempt;

import static ru.sberbank.sbercrm.saas.doctemplate.jooq.tables.TGenerationJobAttempt.T_GENERATION_JOB_ATTEMPT;

@Repository
@RequiredArgsConstructor
public class JooqGenerationJobAttemptRepository implements GenerationJobAttemptRepository {
    private final DSLContext dslContext;
    private final Clock clock;
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
            .set(T_GENERATION_JOB_ATTEMPT.STATUS, DocumentConstants.GenerationJobAttemptStatus.PROCESSING)
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
    public void markCompleted(UUID userId, UUID attemptId) {
        OffsetDateTime now = OffsetDateTime.now(clock);
        dslContext.update(T_GENERATION_JOB_ATTEMPT)
            .set(T_GENERATION_JOB_ATTEMPT.STATUS, DocumentConstants.GenerationJobAttemptStatus.DONE)
            .set(T_GENERATION_JOB_ATTEMPT.FINISHED_AT, now)
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
            .set(T_GENERATION_JOB_ATTEMPT.STATUS, DocumentConstants.GenerationJobAttemptStatus.ERROR)
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
            .set(T_GENERATION_JOB_ATTEMPT.STATUS, DocumentConstants.GenerationJobAttemptStatus.TIMEOUT)
            .set(T_GENERATION_JOB_ATTEMPT.FINISHED_AT, now)
            .set(T_GENERATION_JOB_ATTEMPT.ERROR_CODE, "generation.job_timeout")
            .set(T_GENERATION_JOB_ATTEMPT.ERROR_MESSAGE, "Generation job timed out")
            .set(T_GENERATION_JOB_ATTEMPT.UPDATED_BY, userId)
            .where(T_GENERATION_JOB_ATTEMPT.ID.eq(attemptId))
            .execute();
    }
}
