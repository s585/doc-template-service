package ru.sberbank.sbercrm.saas.doctemplate.document.adapter;

import java.util.UUID;
import ru.sberbank.sbercrm.saas.doctemplate.document.dto.DocumentCreationRq;
import ru.sberbank.sbercrm.saas.doctemplate.document.dto.DocumentRs;
import ru.sberbank.sbercrm.saas.doctemplate.shared.dto.CommonRqDto;
import ru.sberbank.sbercrm.saas.doctemplate.shared.dto.CommonRsDto;

public interface DocumentWebAdapter {
    DocumentRs createDocument(UUID tenantId, UUID userId, DocumentCreationRq request);

    DocumentRs getDocument(UUID tenantId, UUID documentId);

    CommonRsDto listDocuments(UUID tenantId, UUID entityId, UUID objectId, CommonRqDto request);
}
