package ru.sberbank.sbercrm.saas.doctemplate.document.service;

import java.util.List;
import java.util.UUID;

public interface GeneratedFileService {
    void createAll(UUID tenantId, UUID userId, UUID documentId, List<String> formats);

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
