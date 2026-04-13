package ru.sberbank.sbercrm.saas.doctemplate.document.repository;

import java.util.List;
import java.util.UUID;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.GenerationJobAttempt;

/**
 * Табличный repository для {@code t_generation_job_attempt}.
 *
 * <p>Хранит историю попыток выполнения job.
 */
public interface GenerationJobAttemptRepository {
    GenerationJobAttempt create(UUID userId, UUID jobId, UUID workerId);

    List<GenerationJobAttempt> findByJobId(UUID jobId);

    void markCompleted(UUID userId, UUID attemptId);

    void markFailed(UUID userId, UUID attemptId, String errorCode, String errorMessage);

    void markTimedOutActiveAttempt(UUID userId, UUID jobId);
}
