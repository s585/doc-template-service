package ru.sberbank.sbercrm.saas.doctemplate.document.usecase;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.sberbank.sbercrm.saas.doctemplate.application.pagination.PageResult;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.Document;
import ru.sberbank.sbercrm.saas.doctemplate.document.repository.DocumentQueryRepository;
import ru.sberbank.sbercrm.saas.doctemplate.shared.dto.CommonRqDto;

@Component
@RequiredArgsConstructor
public class DocumentListUseCaseImpl implements DocumentListUseCase {
    private final DocumentQueryRepository documentQueryRepository;

    @Override
    public PageResult<Document> execute(UUID tenantId, UUID entityId, UUID objectId, CommonRqDto request) {
        return PageResult.<Document>builder()
            .data(documentQueryRepository.findAllByObject(tenantId, entityId, objectId, request))
            .totalRecordsAmount(documentQueryRepository.countByObject(tenantId, entityId, objectId, request))
            .build();
    }
}
