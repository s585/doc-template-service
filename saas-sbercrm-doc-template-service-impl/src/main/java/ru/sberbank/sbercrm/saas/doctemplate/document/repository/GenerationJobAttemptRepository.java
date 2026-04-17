package ru.sberbank.sbercrm.saas.doctemplate.document.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.GenerationArtifactMeta;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.GenerationJobAttempt;

/**
 * Табличный repository для {@code t_generation_job_attempt}.
 *
 * <p>Хранит историю попыток выполнения job.
 */
public interface GenerationJobAttemptRepository {
    GenerationJobAttempt create(UUID userId, UUID jobId, UUID workerId);

    List<GenerationJobAttempt> findByJobId(UUID jobId);

    void markArtifactUploaded(UUID userId, UUID attemptId, GenerationArtifactMeta artifactMeta);

    void markCompleted(UUID userId, UUID attemptId, GenerationArtifactMeta artifactMeta);

    void markFailed(UUID userId, UUID attemptId, String errorCode, String errorMessage);

    void markTimedOutActiveAttempt(UUID userId, UUID jobId);

    Optional<GenerationArtifactMeta> findLatestArtifactBeforeAttempt(UUID jobId, int attemptNo);
}
