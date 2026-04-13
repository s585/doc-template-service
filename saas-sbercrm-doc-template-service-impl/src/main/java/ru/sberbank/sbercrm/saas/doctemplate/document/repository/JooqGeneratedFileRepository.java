package ru.sberbank.sbercrm.saas.doctemplate.document.repository;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;
import ru.sberbank.sbercrm.saas.doctemplate.document.constant.DocumentConstants;
import ru.sberbank.sbercrm.saas.doctemplate.document.converter.GeneratedFileRecordConverter;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.GeneratedFile;

import static ru.sberbank.sbercrm.saas.doctemplate.jooq.tables.TGeneratedFile.T_GENERATED_FILE;

@Repository
@RequiredArgsConstructor
public class JooqGeneratedFileRepository implements GeneratedFileRepository {
    private final DSLContext dslContext;
    private final Clock clock;
    private final GeneratedFileRecordConverter generatedFileRecordConverter;

    @Override
    public void createAll(UUID tenantId, UUID userId, UUID documentId, List<String> formats) {
        OffsetDateTime now = OffsetDateTime.now(clock);
        dslContext.batch(
            formats.stream()
                .map(format -> dslContext.insertInto(T_GENERATED_FILE)
                    .set(T_GENERATED_FILE.TENANT_ID, tenantId)
                    .set(T_GENERATED_FILE.DOCUMENT_ID, documentId)
                    .set(T_GENERATED_FILE.FORMAT, format)
                    .set(T_GENERATED_FILE.STATUS, DocumentConstants.GeneratedFileStatus.PENDING)
                    .set(T_GENERATED_FILE.CREATED_BY, userId)
                    .set(T_GENERATED_FILE.UPDATED_BY, userId)
                    .set(T_GENERATED_FILE.CREATED_AT, now)
                    .set(T_GENERATED_FILE.UPDATED_AT, now))
                .toList()
        ).execute();
    }

    @Override
    public List<GeneratedFile> findByDocumentId(UUID tenantId, UUID documentId) {
        return dslContext.selectFrom(T_GENERATED_FILE)
            .where(
                T_GENERATED_FILE.TENANT_ID.eq(tenantId),
                T_GENERATED_FILE.DOCUMENT_ID.eq(documentId)
            )
            .orderBy(T_GENERATED_FILE.CREATED_AT.asc(), T_GENERATED_FILE.ID.asc())
            .fetch(generatedFileRecordConverter);
    }

    @Override
    public List<GeneratedFile> findByDocumentIds(UUID tenantId, List<UUID> documentIds) {
        if (documentIds == null || documentIds.isEmpty()) {
            return List.of();
        }

        return dslContext.selectFrom(T_GENERATED_FILE)
            .where(
                T_GENERATED_FILE.TENANT_ID.eq(tenantId),
                T_GENERATED_FILE.DOCUMENT_ID.in(documentIds)
            )
            .orderBy(T_GENERATED_FILE.CREATED_AT.asc(), T_GENERATED_FILE.ID.asc())
            .fetch(generatedFileRecordConverter);
    }

    @Override
    public void markPending(UUID tenantId, UUID userId, UUID documentId, String format) {
        OffsetDateTime now = OffsetDateTime.now(clock);
        dslContext.update(T_GENERATED_FILE)
            .set(T_GENERATED_FILE.STATUS, DocumentConstants.GeneratedFileStatus.PENDING)
            .set(T_GENERATED_FILE.S3_KEY, (String) null)
            .set(T_GENERATED_FILE.CHECKSUM, (String) null)
            .set(T_GENERATED_FILE.SIZE_BYTES, (Long) null)
            .set(T_GENERATED_FILE.ERROR_CODE, (String) null)
            .set(T_GENERATED_FILE.ERROR_MESSAGE, (String) null)
            .set(T_GENERATED_FILE.UPDATED_BY, userId)
            .set(T_GENERATED_FILE.UPDATED_AT, now)
            .where(
                T_GENERATED_FILE.TENANT_ID.eq(tenantId),
                T_GENERATED_FILE.DOCUMENT_ID.eq(documentId),
                T_GENERATED_FILE.FORMAT.eq(format)
            )
            .execute();
    }

    @Override
    public void markProcessing(UUID tenantId, UUID userId, UUID documentId, String format) {
        OffsetDateTime now = OffsetDateTime.now(clock);
        dslContext.update(T_GENERATED_FILE)
            .set(T_GENERATED_FILE.STATUS, DocumentConstants.GeneratedFileStatus.PROCESSING)
            .set(T_GENERATED_FILE.ERROR_CODE, (String) null)
            .set(T_GENERATED_FILE.ERROR_MESSAGE, (String) null)
            .set(T_GENERATED_FILE.UPDATED_BY, userId)
            .set(T_GENERATED_FILE.UPDATED_AT, now)
            .where(
                T_GENERATED_FILE.TENANT_ID.eq(tenantId),
                T_GENERATED_FILE.DOCUMENT_ID.eq(documentId),
                T_GENERATED_FILE.FORMAT.eq(format)
            )
            .execute();
    }

    @Override
    public void markCompleted(
        UUID tenantId,
        UUID userId,
        UUID documentId,
        String format,
        String s3Key,
        String checksum,
        long sizeBytes
    ) {
        OffsetDateTime now = OffsetDateTime.now(clock);
        dslContext.update(T_GENERATED_FILE)
            .set(T_GENERATED_FILE.STATUS, DocumentConstants.GeneratedFileStatus.DONE)
            .set(T_GENERATED_FILE.S3_KEY, s3Key)
            .set(T_GENERATED_FILE.CHECKSUM, checksum)
            .set(T_GENERATED_FILE.SIZE_BYTES, sizeBytes)
            .set(T_GENERATED_FILE.ERROR_CODE, (String) null)
            .set(T_GENERATED_FILE.ERROR_MESSAGE, (String) null)
            .set(T_GENERATED_FILE.UPDATED_BY, userId)
            .set(T_GENERATED_FILE.UPDATED_AT, now)
            .where(
                T_GENERATED_FILE.TENANT_ID.eq(tenantId),
                T_GENERATED_FILE.DOCUMENT_ID.eq(documentId),
                T_GENERATED_FILE.FORMAT.eq(format)
            )
            .execute();
    }

    @Override
    public void markFailed(UUID tenantId, UUID userId, UUID documentId, String format, String errorCode, String errorMessage) {
        OffsetDateTime now = OffsetDateTime.now(clock);
        dslContext.update(T_GENERATED_FILE)
            .set(T_GENERATED_FILE.STATUS, DocumentConstants.GeneratedFileStatus.ERROR)
            .set(T_GENERATED_FILE.ERROR_CODE, errorCode)
            .set(T_GENERATED_FILE.ERROR_MESSAGE, errorMessage)
            .set(T_GENERATED_FILE.UPDATED_BY, userId)
            .set(T_GENERATED_FILE.UPDATED_AT, now)
            .where(
                T_GENERATED_FILE.TENANT_ID.eq(tenantId),
                T_GENERATED_FILE.DOCUMENT_ID.eq(documentId),
                T_GENERATED_FILE.FORMAT.eq(format)
            )
            .execute();
    }
}
