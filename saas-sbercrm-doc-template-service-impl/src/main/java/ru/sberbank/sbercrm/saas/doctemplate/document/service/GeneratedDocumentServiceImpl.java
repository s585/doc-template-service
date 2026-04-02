package ru.sberbank.sbercrm.saas.doctemplate.document.service;

import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.sberbank.sbercrm.saas.doctemplate.application.pagination.PageResult;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.DocumentCreationCmd;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.GeneratedDocument;
import ru.sberbank.sbercrm.saas.doctemplate.document.repository.GeneratedDocumentRepository;
import ru.sberbank.sbercrm.saas.doctemplate.shared.dto.CommonRqDto;

@Service
@RequiredArgsConstructor
public class GeneratedDocumentServiceImpl implements GeneratedDocumentService {
    private final GeneratedDocumentRepository generatedDocumentRepository;

    @Override
    public GeneratedDocument create(UUID tenantId, UUID userId, DocumentCreationCmd command) {
        return generatedDocumentRepository.create(tenantId, userId, command);
    }

    @Override
    public Optional<GeneratedDocument> findById(UUID tenantId, UUID documentId) {
        return generatedDocumentRepository.findById(tenantId, documentId);
    }

    @Override
    public Optional<GeneratedDocument> findByRequestId(UUID tenantId, UUID requestId) {
        return generatedDocumentRepository.findByRequestId(tenantId, requestId);
    }

    @Override
    public PageResult<GeneratedDocument> findAllByObject(UUID tenantId, UUID entityId, UUID objectId, CommonRqDto request) {
        return PageResult.<GeneratedDocument>builder()
            .data(generatedDocumentRepository.findAllByObject(tenantId, entityId, objectId, request))
            .totalRecordsAmount(generatedDocumentRepository.countByObject(tenantId, entityId, objectId, request))
            .build();
    }
}
