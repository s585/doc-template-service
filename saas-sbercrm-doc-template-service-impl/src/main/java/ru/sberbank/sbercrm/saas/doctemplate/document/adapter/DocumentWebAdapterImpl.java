package ru.sberbank.sbercrm.saas.doctemplate.document.adapter;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.sberbank.sbercrm.saas.doctemplate.application.pagination.CommonPageResponseBuilder;
import ru.sberbank.sbercrm.saas.doctemplate.document.converter.DocumentConverter;
import ru.sberbank.sbercrm.saas.doctemplate.document.dto.DocumentCreationRq;
import ru.sberbank.sbercrm.saas.doctemplate.document.dto.DocumentRs;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.Document;
import ru.sberbank.sbercrm.saas.doctemplate.document.usecase.DocumentCreationUseCase;
import ru.sberbank.sbercrm.saas.doctemplate.document.usecase.DocumentGetUseCase;
import ru.sberbank.sbercrm.saas.doctemplate.document.usecase.DocumentListUseCase;
import ru.sberbank.sbercrm.saas.doctemplate.shared.dto.CommonRqDto;
import ru.sberbank.sbercrm.saas.doctemplate.shared.dto.CommonRsDto;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class DocumentWebAdapterImpl implements DocumentWebAdapter {
    private final DocumentConverter documentConverter;
    private final DocumentCreationUseCase documentCreationUseCase;
    private final DocumentGetUseCase documentGetUseCase;
    private final DocumentListUseCase documentListUseCase;

    @Override
    public DocumentRs createDocument(UUID tenantId, UUID userId, DocumentCreationRq request) {
        Document document = documentCreationUseCase.execute(
            tenantId,
            userId,
            documentConverter.convertToModel(request)
        );
        return documentConverter.convertToRs(document);
    }

    @Override
    public DocumentRs getDocument(UUID tenantId, UUID documentId) {
        return documentConverter.convertToRs(documentGetUseCase.execute(tenantId, documentId));
    }

    @Override
    public CommonRsDto listDocuments(UUID tenantId, UUID entityId, UUID objectId, CommonRqDto request) {
        return CommonPageResponseBuilder.build(
            request,
            documentListUseCase.execute(tenantId, entityId, objectId, request),
            documentConverter::convertToRs
        );
    }
}
