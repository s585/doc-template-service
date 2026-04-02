package ru.sberbank.sbercrm.saas.doctemplate.document.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.DocumentCreationCmd;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.GeneratedDocument;
import ru.sberbank.sbercrm.saas.doctemplate.shared.dto.CommonRqDto;

public interface GeneratedDocumentRepository {
    GeneratedDocument create(UUID tenantId, UUID userId, DocumentCreationCmd command);

    Optional<GeneratedDocument> findById(UUID tenantId, UUID documentId);

    Optional<GeneratedDocument> findByRequestId(UUID tenantId, UUID requestId);

    List<GeneratedDocument> findAllByObject(UUID tenantId, UUID entityId, UUID objectId, CommonRqDto request);

    long countByObject(UUID tenantId, UUID entityId, UUID objectId, CommonRqDto request);
}
