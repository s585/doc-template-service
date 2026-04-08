package ru.sberbank.sbercrm.saas.doctemplate.document.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.Document;
import ru.sberbank.sbercrm.saas.doctemplate.document.repository.DocumentQueryRepository;
import ru.sberbank.sbercrm.saas.doctemplate.shared.dto.CommonRqDto;

@Service
@RequiredArgsConstructor
public class DocumentQueryServiceImpl implements DocumentQueryService {
    private final DocumentQueryRepository documentQueryRepository;

    @Override
    public Optional<Document> findById(UUID tenantId, UUID documentId) {
        return documentQueryRepository.findById(tenantId, documentId);
    }

    @Override
    public List<Document> findAllByObject(UUID tenantId, UUID entityId, UUID objectId, CommonRqDto request) {
        return documentQueryRepository.findAllByObject(tenantId, entityId, objectId, request);
    }

    @Override
    public long countByObject(UUID tenantId, UUID entityId, UUID objectId, CommonRqDto request) {
        return documentQueryRepository.countByObject(tenantId, entityId, objectId, request);
    }
}
