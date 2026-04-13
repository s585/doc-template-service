package ru.sberbank.sbercrm.saas.doctemplate.document.service;

import java.util.List;
import java.util.UUID;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.GenerationJobAttempt;

/**
 * Сервис работы с журналом попыток выполнения {@code generation_job}.
 *
 * <p>{@code generation_job_attempt} хранит operational history попыток выполнения job.
 */
public interface GenerationJobAttemptService {
    /**
     * Создаёт новую активную attempt для уже claim-нутой job.
     */
    GenerationJobAttempt create(UUID userId, UUID jobId, UUID workerId);

    List<GenerationJobAttempt> findByJobId(UUID jobId);

    void markCompleted(UUID userId, UUID attemptId);

    void markFailed(UUID userId, UUID attemptId, String errorCode, String errorMessage);

    /**
     * Помечает последнюю незавершённую attempt как timed out.
     */
    void markTimedOutActiveAttempt(UUID userId, UUID jobId);
}
