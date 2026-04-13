package ru.sberbank.sbercrm.saas.doctemplate.document.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.DocumentCreationCmd;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.GenerationJob;
import ru.sberbank.sbercrm.saas.doctemplate.document.repository.GenerationJobRepository;

@Service
@RequiredArgsConstructor
public class GenerationJobServiceImpl implements GenerationJobService {
    private final GenerationJobRepository generationJobRepository;

    @Override
    public void createAll(UUID tenantId, UUID userId, UUID documentId, DocumentCreationCmd command) {
        generationJobRepository.createAll(tenantId, userId, documentId, command);
    }

    @Override
    public Optional<GenerationJob> findById(UUID tenantId, UUID jobId) {
        return generationJobRepository.findById(tenantId, jobId);
    }

    @Override
    public List<GenerationJob> findTimedOutJobs() {
        return generationJobRepository.findTimedOutJobs();
    }

    @Override
    public List<GenerationJob> findByDocumentId(UUID tenantId, UUID documentId) {
        return generationJobRepository.findByDocumentId(tenantId, documentId);
    }

    @Override
    public Map<UUID, List<GenerationJob>> findByDocumentIds(UUID tenantId, Collection<UUID> documentIds) {
        return generationJobRepository.findByDocumentIds(tenantId, documentIds);
    }

    @Override
    public List<GenerationJob> claimNextJobs(UUID workerId, int limit) {
        return generationJobRepository.claimNextJobs(workerId, limit);
    }

    @Override
    public boolean scheduleRetry(
        UUID tenantId,
        UUID userId,
        UUID jobId,
        int expectedAttemptCount,
        int attemptCount,
        OffsetDateTime nextRetryAt,
        String errorCode,
        String errorMessage
    ) {
        return generationJobRepository.scheduleRetry(
            tenantId,
            userId,
            jobId,
            expectedAttemptCount,
            attemptCount,
            nextRetryAt,
            errorCode,
            errorMessage
        );
    }

    @Override
    public boolean markCompleted(
        UUID tenantId,
        UUID userId,
        UUID jobId,
        int expectedAttemptCount,
        int attemptCount
    ) {
        return generationJobRepository.markCompleted(
            tenantId,
            userId,
            jobId,
            expectedAttemptCount,
            attemptCount
        );
    }

    @Override
    public boolean markFailed(
        UUID tenantId,
        UUID userId,
        UUID jobId,
        int expectedAttemptCount,
        int attemptCount,
        String errorCode,
        String errorMessage
    ) {
        return generationJobRepository.markFailed(
            tenantId,
            userId,
            jobId,
            expectedAttemptCount,
            attemptCount,
            errorCode,
            errorMessage
        );
    }
}
