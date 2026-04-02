package ru.sberbank.sbercrm.saas.doctemplate.document.repository;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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
import static ru.sberbank.sbercrm.jooq.tables.TGenerationJob.T_GENERATION_JOB;

@Repository
@RequiredArgsConstructor
public class JooqGenerationJobRepository implements GenerationJobRepository {
    private final DSLContext dslContext;
    private final GenerationJobRecordConverter generationJobRecordConverter;

    @Override
    public void createAll(UUID tenantId, UUID userId, UUID documentId, DocumentCreationCmd command) {
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
                    .set(T_GENERATION_JOB.CREATED_BY, userId)
                    .set(T_GENERATION_JOB.UPDATED_BY, userId))
                .toList()
        ).execute();
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

        return dslContext.transactionResult(configuration -> {
            DSLContext tx = DSL.using(configuration);
            List<UUID> jobIds = tx.select(T_GENERATION_JOB.ID)
                .from(T_GENERATION_JOB)
                .where(
                    T_GENERATION_JOB.STATUS.eq(DocumentConstants.GenerationJobStatus.QUEUED),
                    T_GENERATION_JOB.LOCKED_UNTIL.isNull().or(T_GENERATION_JOB.LOCKED_UNTIL.lt(DSL.currentOffsetDateTime()))
                )
                .orderBy(T_GENERATION_JOB.CREATED_AT.asc(), T_GENERATION_JOB.ID.asc())
                .limit(limit)
                .forUpdate()
                .skipLocked()
                .fetch(T_GENERATION_JOB.ID);

            if (jobIds.isEmpty()) {
                return List.<GenerationJob>of();
            }

            tx.update(T_GENERATION_JOB)
                .set(T_GENERATION_JOB.STATUS, DocumentConstants.GenerationJobStatus.PROCESSING)
                .set(T_GENERATION_JOB.LOCKED_BY, workerId)
                .set(
                    T_GENERATION_JOB.LOCKED_UNTIL,
                    DSL.field("{0} + interval '5 minutes'", java.time.OffsetDateTime.class, DSL.currentOffsetDateTime())
                )
                .set(T_GENERATION_JOB.ERROR_CODE, (String) null)
                .set(T_GENERATION_JOB.ERROR_MESSAGE, (String) null)
                .set(T_GENERATION_JOB.UPDATED_BY, workerId)
                .set(T_GENERATION_JOB.UPDATED_AT, DSL.currentOffsetDateTime())
                .where(T_GENERATION_JOB.ID.in(jobIds))
                .execute();

            return tx.selectFrom(T_GENERATION_JOB)
                .where(T_GENERATION_JOB.ID.in(jobIds))
                .orderBy(T_GENERATION_JOB.CREATED_AT.asc(), T_GENERATION_JOB.ID.asc())
                .fetch(generationJobRecordConverter);
        });
    }

    @Override
    public void markCompleted(UUID tenantId, UUID userId, UUID jobId) {
        dslContext.update(T_GENERATION_JOB)
            .set(T_GENERATION_JOB.STATUS, DocumentConstants.GenerationJobStatus.DONE)
            .set(T_GENERATION_JOB.LOCKED_BY, (UUID) null)
            .set(T_GENERATION_JOB.LOCKED_UNTIL, (java.time.OffsetDateTime) null)
            .set(T_GENERATION_JOB.ERROR_CODE, (String) null)
            .set(T_GENERATION_JOB.ERROR_MESSAGE, (String) null)
            .set(T_GENERATION_JOB.UPDATED_BY, userId)
            .set(T_GENERATION_JOB.UPDATED_AT, DSL.currentOffsetDateTime())
            .where(
                T_GENERATION_JOB.TENANT_ID.eq(tenantId),
                T_GENERATION_JOB.ID.eq(jobId)
            )
            .execute();
    }

    @Override
    public void markFailed(UUID tenantId, UUID userId, UUID jobId, String errorCode, String errorMessage) {
        dslContext.update(T_GENERATION_JOB)
            .set(T_GENERATION_JOB.STATUS, DocumentConstants.GenerationJobStatus.ERROR)
            .set(T_GENERATION_JOB.LOCKED_BY, (UUID) null)
            .set(T_GENERATION_JOB.LOCKED_UNTIL, (java.time.OffsetDateTime) null)
            .set(T_GENERATION_JOB.ERROR_CODE, errorCode)
            .set(T_GENERATION_JOB.ERROR_MESSAGE, errorMessage)
            .set(T_GENERATION_JOB.UPDATED_BY, userId)
            .set(T_GENERATION_JOB.UPDATED_AT, DSL.currentOffsetDateTime())
            .where(
                T_GENERATION_JOB.TENANT_ID.eq(tenantId),
                T_GENERATION_JOB.ID.eq(jobId)
            )
            .execute();
    }
}
