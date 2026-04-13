package ru.sberbank.sbercrm.saas.doctemplate.document.repository;

import java.util.List;
import java.util.UUID;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.GeneratedFile;

public interface GeneratedFileRepository {
    void createAll(UUID tenantId, UUID userId, UUID documentId, List<String> formats);

    List<GeneratedFile> findByDocumentId(UUID tenantId, UUID documentId);

    List<GeneratedFile> findByDocumentIds(UUID tenantId, List<UUID> documentIds);

    void markPending(UUID tenantId, UUID userId, UUID documentId, String format);

    void markProcessing(UUID tenantId, UUID userId, UUID documentId, String format);

    void markCompleted(
        UUID tenantId,
        UUID userId,
        UUID documentId,
        String format,
        String s3Key,
        String checksum,
        long sizeBytes
    );

    void markFailed(UUID tenantId, UUID userId, UUID documentId, String format, String errorCode, String errorMessage);
}
