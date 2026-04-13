package ru.sberbank.sbercrm.saas.doctemplate.document.service;

import java.util.List;
import java.util.UUID;

/**
 * Сервис работы с состоянием {@code generated_file}.
 *
 * <p>Используется transition-слоем для синхронного отражения статуса generation job во внешнем файле.
 */
public interface GeneratedFileService {
    void createAll(UUID tenantId, UUID userId, UUID documentId, List<String> formats);

    /**
     * Возвращает файл в ожидающее состояние перед повторной попыткой.
     */
    void markPending(UUID tenantId, UUID userId, UUID documentId, String format);

    void markProcessing(UUID tenantId, UUID userId, UUID documentId, String format);

    /**
     * Фиксирует успешное создание физического результата генерации.
     */
    void markCompleted(
        UUID tenantId,
        UUID userId,
        UUID documentId,
        String format,
        String s3Key,
        String checksum,
        long sizeBytes
    );

    /**
     * Фиксирует финальную ошибку генерации файла.
     */
    void markFailed(
        UUID tenantId,
        UUID userId,
        UUID documentId,
        String format,
        String errorCode,
        String errorMessage
    );
}
