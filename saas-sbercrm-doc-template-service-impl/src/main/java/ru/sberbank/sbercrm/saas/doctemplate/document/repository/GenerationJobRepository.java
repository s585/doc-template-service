package ru.sberbank.sbercrm.saas.doctemplate.document.repository;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.DocumentCreationCmd;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.GenerationJob;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.GenerationJobRetryCmd;

/**
 * Табличный repository для {@code t_generation_job}.
 *
 * <p>Работает только с таблицей job.
 */
public interface GenerationJobRepository {
    void createAll(UUID tenantId, UUID userId, UUID documentId, DocumentCreationCmd command);

    Optional<GenerationJob> findById(UUID tenantId, UUID jobId);

    List<GenerationJob> findTimedOutJobs();

    /**
     * Claim-ит следующие доступные job и переводит их в {@code PROCESSING}.
     */
    List<GenerationJob> claimNextJobs(UUID workerId, int limit);

    /**
     * Планирует retriable повтор и атомарно обновляет счётчик попыток.
     *
     * <p>Запись меняется только если job всё ещё находится в ожидаемом состоянии той же попытки.
     */
    boolean scheduleRetry(GenerationJobRetryCmd retryCmd);

    /**
     * Завершает job успехом только для ожидаемой активной попытки.
     */
    boolean markCompleted(
        UUID tenantId,
        UUID userId,
        UUID jobId,
        int expectedAttemptCount,
        int attemptCount
    );

    /**
     * Завершает job финальной ошибкой только для ожидаемой активной попытки.
     */
    boolean markFailed(
        UUID tenantId,
        UUID userId,
        UUID jobId,
        int expectedAttemptCount,
        int attemptCount,
        String errorCode,
        String errorMessage
    );
}
