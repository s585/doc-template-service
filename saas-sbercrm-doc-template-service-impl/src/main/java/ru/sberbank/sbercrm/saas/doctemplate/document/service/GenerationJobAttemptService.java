package ru.sberbank.sbercrm.saas.doctemplate.document.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.GenerationArtifactMeta;
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

    void markArtifactUploaded(UUID userId, UUID attemptId, GenerationArtifactMeta artifactMeta);

    void markCompleted(UUID userId, UUID attemptId, GenerationArtifactMeta artifactMeta);

    void markFailed(UUID userId, UUID attemptId, String errorCode, String errorMessage);

    /**
     * Помечает последнюю незавершённую attempt как timed out.
     */
    void markTimedOutActiveAttempt(UUID userId, UUID jobId);

    Optional<GenerationArtifactMeta> findLatestArtifactBeforeAttempt(UUID jobId, int attemptNo);
}
