package ru.sberbank.sbercrm.saas.doctemplate.document.repository;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;
import ru.sberbank.sbercrm.saas.doctemplate.document.constant.DocumentConstants;
import ru.sberbank.sbercrm.saas.doctemplate.document.converter.GenerationJobRecordConverter;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.DocumentCreationCmd;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.GenerationJob;

import static java.util.stream.Collectors.groupingBy;
import static ru.sberbank.sbercrm.saas.doctemplate.jooq.tables.TGenerationJob.T_GENERATION_JOB;

@Repository
@RequiredArgsConstructor
public class JooqGenerationJobRepository implements GenerationJobRepository {
    private final DSLContext dslContext;
    private final Clock clock;
    private final GenerationJobRecordConverter generationJobRecordConverter;

    @Override
    public void createAll(UUID tenantId, UUID userId, UUID documentId, DocumentCreationCmd command) {
        OffsetDateTime now = OffsetDateTime.now(clock);
        dslContext.batch(
            command.getFormats().stream()
                .map(format -> dslContext.insertInto(T_GENERATION_JOB)
                    .set(T_GENERATION_JOB.TENANT_ID, tenantId)
                    .set(T_GENERATION_JOB.DOCUMENT_ID, documentId)
                    .set(T_GENERATION_JOB.TEMPLATE_ID, command.getTemplateId())
                    .set(T_GENERATION_JOB.ENTITY_ID, command.getEntityId())
                    .set(T_GENERATION_JOB.OBJECT_ID, command.getObjectId())
                    .set(T_GENERATION_JOB.FORMAT, format)
                    .set(T_GENERATION_JOB.STATUS, DocumentConstants.GenerationJobStatus.QUEUED)
                    .set(T_GENERATION_JOB.ATTEMPT_COUNT, 0)
                    .set(T_GENERATION_JOB.CREATED_BY, userId)
                    .set(T_GENERATION_JOB.UPDATED_BY, userId)
                    .set(T_GENERATION_JOB.CREATED_AT, now)
                    .set(T_GENERATION_JOB.UPDATED_AT, now))
                .toList()
        ).execute();
    }

    @Override
    public Optional<GenerationJob> findById(UUID tenantId, UUID jobId) {
        return dslContext.selectFrom(T_GENERATION_JOB)
            .where(
                T_GENERATION_JOB.TENANT_ID.eq(tenantId),
                T_GENERATION_JOB.ID.eq(jobId)
            )
            .fetchOptional(generationJobRecordConverter);
    }

    @Override
    public List<GenerationJob> findTimedOutJobs() {
        OffsetDateTime now = OffsetDateTime.now(clock);
        return dslContext.selectFrom(T_GENERATION_JOB)
            .where(
                T_GENERATION_JOB.STATUS.eq(DocumentConstants.GenerationJobStatus.PROCESSING),
                T_GENERATION_JOB.LOCKED_UNTIL.isNotNull(),
                T_GENERATION_JOB.LOCKED_UNTIL.lt(now)
            )
            .orderBy(T_GENERATION_JOB.LOCKED_UNTIL.asc(), T_GENERATION_JOB.ID.asc())
            .fetch(generationJobRecordConverter);
    }

    @Override
    public List<GenerationJob> findByDocumentId(UUID tenantId, UUID documentId) {
        return dslContext.selectFrom(T_GENERATION_JOB)
            .where(
                T_GENERATION_JOB.TENANT_ID.eq(tenantId),
                T_GENERATION_JOB.DOCUMENT_ID.eq(documentId)
            )
            .orderBy(T_GENERATION_JOB.CREATED_AT.asc(), T_GENERATION_JOB.ID.asc())
            .fetch(generationJobRecordConverter);
    }

    @Override
    public Map<UUID, List<GenerationJob>> findByDocumentIds(UUID tenantId, Collection<UUID> documentIds) {
        if (documentIds == null || documentIds.isEmpty()) {
            return Collections.emptyMap();
        }

        return dslContext.selectFrom(T_GENERATION_JOB)
            .where(
                T_GENERATION_JOB.TENANT_ID.eq(tenantId),
                T_GENERATION_JOB.DOCUMENT_ID.in(documentIds)
            )
            .orderBy(T_GENERATION_JOB.CREATED_AT.asc(), T_GENERATION_JOB.ID.asc())
            .fetch(generationJobRecordConverter)
            .stream()
            .collect(groupingBy(GenerationJob::getDocumentId));
    }

    @Override
    public List<GenerationJob> claimNextJobs(UUID workerId, int limit) {
        if (limit <= 0) {
            return List.of();
        }

        OffsetDateTime now = OffsetDateTime.now(clock);
        OffsetDateTime lockedUntil = now.plusMinutes(5);

        return dslContext.transactionResult(configuration -> {
            DSLContext tx = DSL.using(configuration);
            List<UUID> jobIds = findNextClaimableJobIds(tx, now, limit);

            if (jobIds.isEmpty()) {
                return List.<GenerationJob>of();
            }

            markJobsProcessing(tx, workerId, now, lockedUntil, jobIds);

            return findClaimedJobs(tx, jobIds);
        });
    }

    private List<UUID> findNextClaimableJobIds(DSLContext tx, OffsetDateTime now, int limit) {
        return tx.select(T_GENERATION_JOB.ID)
            .from(T_GENERATION_JOB)
            .where(
                T_GENERATION_JOB.STATUS.eq(DocumentConstants.GenerationJobStatus.QUEUED),
                T_GENERATION_JOB.NEXT_RETRY_AT.isNull().or(T_GENERATION_JOB.NEXT_RETRY_AT.le(now)),
                T_GENERATION_JOB.LOCKED_UNTIL.isNull().or(T_GENERATION_JOB.LOCKED_UNTIL.lt(now))
            )
            .orderBy(
                T_GENERATION_JOB.NEXT_RETRY_AT.asc().nullsFirst(),
                T_GENERATION_JOB.CREATED_AT.asc(),
                T_GENERATION_JOB.ID.asc()
            )
            .limit(limit)
            .forUpdate()
            .skipLocked()
            .fetch(T_GENERATION_JOB.ID);
    }

    private void markJobsProcessing(
        DSLContext tx,
        UUID workerId,
        OffsetDateTime now,
        OffsetDateTime lockedUntil,
        List<UUID> jobIds
    ) {
        tx.update(T_GENERATION_JOB)
            .set(T_GENERATION_JOB.STATUS, DocumentConstants.GenerationJobStatus.PROCESSING)
            .set(T_GENERATION_JOB.LOCKED_BY, workerId)
            .set(T_GENERATION_JOB.LOCKED_UNTIL, lockedUntil)
            .set(T_GENERATION_JOB.ERROR_CODE, (String) null)
            .set(T_GENERATION_JOB.ERROR_MESSAGE, (String) null)
            .set(T_GENERATION_JOB.UPDATED_BY, workerId)
            .set(T_GENERATION_JOB.UPDATED_AT, now)
            .where(T_GENERATION_JOB.ID.in(jobIds))
            .execute();
    }

    private List<GenerationJob> findClaimedJobs(DSLContext tx, List<UUID> jobIds) {
        return tx.selectFrom(T_GENERATION_JOB)
            .where(T_GENERATION_JOB.ID.in(jobIds))
            .orderBy(T_GENERATION_JOB.CREATED_AT.asc(), T_GENERATION_JOB.ID.asc())
            .fetch(generationJobRecordConverter);
    }

    @Override
    public boolean scheduleRetry(
        UUID tenantId,
        UUID userId,
        UUID jobId,
        int expectedAttemptCount,
        int attemptCount,
        OffsetDateTime nextRetryAt,
        String errorCode,
        String errorMessage
    ) {
        OffsetDateTime now = OffsetDateTime.now(clock);
        return dslContext.update(T_GENERATION_JOB)
            .set(T_GENERATION_JOB.STATUS, DocumentConstants.GenerationJobStatus.QUEUED)
            .set(T_GENERATION_JOB.ATTEMPT_COUNT, attemptCount)
            .set(T_GENERATION_JOB.NEXT_RETRY_AT, nextRetryAt)
            .set(T_GENERATION_JOB.LOCKED_BY, (UUID) null)
            .set(T_GENERATION_JOB.LOCKED_UNTIL, (OffsetDateTime) null)
            .set(T_GENERATION_JOB.ERROR_CODE, errorCode)
            .set(T_GENERATION_JOB.ERROR_MESSAGE, errorMessage)
            .set(T_GENERATION_JOB.UPDATED_BY, userId)
            .set(T_GENERATION_JOB.UPDATED_AT, now)
            .where(
                T_GENERATION_JOB.TENANT_ID.eq(tenantId),
                T_GENERATION_JOB.ID.eq(jobId),
                T_GENERATION_JOB.STATUS.eq(DocumentConstants.GenerationJobStatus.PROCESSING),
                T_GENERATION_JOB.ATTEMPT_COUNT.eq(expectedAttemptCount)
            )
            .execute() == 1;
    }

    @Override
    public boolean markCompleted(
        UUID tenantId,
        UUID userId,
        UUID jobId,
        int expectedAttemptCount,
        int attemptCount
    ) {
        OffsetDateTime now = OffsetDateTime.now(clock);
        return dslContext.update(T_GENERATION_JOB)
            .set(T_GENERATION_JOB.STATUS, DocumentConstants.GenerationJobStatus.DONE)
            .set(T_GENERATION_JOB.ATTEMPT_COUNT, attemptCount)
            .set(T_GENERATION_JOB.NEXT_RETRY_AT, (OffsetDateTime) null)
            .set(T_GENERATION_JOB.LOCKED_BY, (UUID) null)
            .set(T_GENERATION_JOB.LOCKED_UNTIL, (java.time.OffsetDateTime) null)
            .set(T_GENERATION_JOB.ERROR_CODE, (String) null)
            .set(T_GENERATION_JOB.ERROR_MESSAGE, (String) null)
            .set(T_GENERATION_JOB.UPDATED_BY, userId)
            .set(T_GENERATION_JOB.UPDATED_AT, now)
            .where(
                T_GENERATION_JOB.TENANT_ID.eq(tenantId),
                T_GENERATION_JOB.ID.eq(jobId),
                T_GENERATION_JOB.STATUS.eq(DocumentConstants.GenerationJobStatus.PROCESSING),
                T_GENERATION_JOB.ATTEMPT_COUNT.eq(expectedAttemptCount)
            )
            .execute() == 1;
    }

    @Override
    public boolean markFailed(
        UUID tenantId,
        UUID userId,
        UUID jobId,
        int expectedAttemptCount,
        int attemptCount,
        String errorCode,
        String errorMessage
    ) {
        OffsetDateTime now = OffsetDateTime.now(clock);
        return dslContext.update(T_GENERATION_JOB)
            .set(T_GENERATION_JOB.STATUS, DocumentConstants.GenerationJobStatus.ERROR)
            .set(T_GENERATION_JOB.ATTEMPT_COUNT, attemptCount)
            .set(T_GENERATION_JOB.NEXT_RETRY_AT, (OffsetDateTime) null)
            .set(T_GENERATION_JOB.LOCKED_BY, (UUID) null)
            .set(T_GENERATION_JOB.LOCKED_UNTIL, (java.time.OffsetDateTime) null)
            .set(T_GENERATION_JOB.ERROR_CODE, errorCode)
            .set(T_GENERATION_JOB.ERROR_MESSAGE, errorMessage)
            .set(T_GENERATION_JOB.UPDATED_BY, userId)
            .set(T_GENERATION_JOB.UPDATED_AT, now)
            .where(
                T_GENERATION_JOB.TENANT_ID.eq(tenantId),
                T_GENERATION_JOB.ID.eq(jobId),
                T_GENERATION_JOB.STATUS.eq(DocumentConstants.GenerationJobStatus.PROCESSING),
                T_GENERATION_JOB.ATTEMPT_COUNT.eq(expectedAttemptCount)
            )
            .execute() == 1;
    }
}
