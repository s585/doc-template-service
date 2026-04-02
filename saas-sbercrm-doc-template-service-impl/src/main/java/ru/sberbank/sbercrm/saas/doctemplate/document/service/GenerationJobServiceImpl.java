package ru.sberbank.sbercrm.saas.doctemplate.document.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
    public void markCompleted(UUID tenantId, UUID userId, UUID jobId) {
        generationJobRepository.markCompleted(tenantId, userId, jobId);
    }

    @Override
    public void markFailed(UUID tenantId, UUID userId, UUID jobId, String errorCode, String errorMessage) {
        generationJobRepository.markFailed(tenantId, userId, jobId, errorCode, errorMessage);
    }
}
