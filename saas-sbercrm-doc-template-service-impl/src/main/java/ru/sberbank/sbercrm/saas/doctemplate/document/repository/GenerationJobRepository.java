package ru.sberbank.sbercrm.saas.doctemplate.document.repository;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.DocumentCreationCmd;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.GenerationJob;

public interface GenerationJobRepository {
    void createAll(UUID tenantId, UUID userId, UUID documentId, DocumentCreationCmd command);

    List<GenerationJob> findByDocumentId(UUID tenantId, UUID documentId);

    Map<UUID, List<GenerationJob>> findByDocumentIds(UUID tenantId, Collection<UUID> documentIds);

    List<GenerationJob> claimNextJobs(UUID workerId, int limit);

    void markCompleted(UUID tenantId, UUID userId, UUID jobId);

    void markFailed(UUID tenantId, UUID userId, UUID jobId, String errorCode, String errorMessage);
}
