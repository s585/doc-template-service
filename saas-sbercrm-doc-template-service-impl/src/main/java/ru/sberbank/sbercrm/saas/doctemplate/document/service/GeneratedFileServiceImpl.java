package ru.sberbank.sbercrm.saas.doctemplate.document.service;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.sberbank.sbercrm.saas.doctemplate.document.repository.GeneratedFileRepository;

@Service
@RequiredArgsConstructor
public class GeneratedFileServiceImpl implements GeneratedFileService {
    private final GeneratedFileRepository generatedFileRepository;

    @Override
    public void createAll(UUID tenantId, UUID userId, UUID documentId, List<String> formats) {
        generatedFileRepository.createAll(tenantId, userId, documentId, formats);
    }

    @Override
    public void markPending(UUID tenantId, UUID userId, UUID documentId, String format) {
        generatedFileRepository.markPending(tenantId, userId, documentId, format);
    }

    @Override
    public void markProcessing(UUID tenantId, UUID userId, UUID documentId, String format) {
        generatedFileRepository.markProcessing(tenantId, userId, documentId, format);
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
        generatedFileRepository.markCompleted(tenantId, userId, documentId, format, s3Key, checksum, sizeBytes);
    }

    @Override
    public void markFailed(UUID tenantId, UUID userId, UUID documentId, String format, String errorCode, String errorMessage) {
        generatedFileRepository.markFailed(tenantId, userId, documentId, format, errorCode, errorMessage);
    }
}
