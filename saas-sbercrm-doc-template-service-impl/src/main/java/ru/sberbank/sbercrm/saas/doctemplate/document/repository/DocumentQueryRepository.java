package ru.sberbank.sbercrm.saas.doctemplate.document.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.Document;
import ru.sberbank.sbercrm.saas.doctemplate.shared.dto.CommonRqDto;

public interface DocumentQueryRepository {
    Optional<Document> findById(UUID tenantId, UUID documentId);

    List<Document> findAllByObject(UUID tenantId, UUID entityId, UUID objectId, CommonRqDto request);

    long countByObject(UUID tenantId, UUID entityId, UUID objectId, CommonRqDto request);
}
