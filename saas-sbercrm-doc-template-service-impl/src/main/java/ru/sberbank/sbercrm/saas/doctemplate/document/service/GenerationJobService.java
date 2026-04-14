package ru.sberbank.sbercrm.saas.doctemplate.document.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.DocumentCreationCmd;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.GenerationJob;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.GenerationJobRetryCmd;

/**
 * Сервис доступа к табличной модели {@code generation_job}.
 *
 * <p>Не агрегирует документ и не принимает orchestration-решения.
 */
public interface GenerationJobService {
    void createAll(UUID tenantId, UUID userId, UUID documentId, DocumentCreationCmd command);

    Optional<GenerationJob> findById(UUID tenantId, UUID jobId);

    List<GenerationJob> findTimedOutJobs();

    /**
     * Атомарно claim-ит следующую пачку job под конкретный worker.
     *
     * <p>Возвращаются только job, реально переведённые в {@code PROCESSING}.
     */
    List<GenerationJob> claimNextJobs(UUID workerId, int limit);

    /**
     * Планирует retriable повтор и увеличивает счётчик попыток только для ожидаемой активной job.
     */
    boolean scheduleRetry(GenerationJobRetryCmd retryCmd);

    /**
     * Фиксирует успешное завершение только если завершается актуальная активная попытка.
     */
    boolean markCompleted(
        UUID tenantId,
        UUID userId,
        UUID jobId,
        int expectedAttemptCount,
        int attemptCount
    );

    /**
     * Фиксирует финальную ошибку только если завершается актуальная активная попытка.
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
