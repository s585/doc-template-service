package ru.sberbank.sbercrm.saas.doctemplate.document.usecase;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.sberbank.sbercrm.saas.doctemplate.application.pagination.PageResult;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.Document;
import ru.sberbank.sbercrm.saas.doctemplate.document.service.DocumentQueryService;
import ru.sberbank.sbercrm.saas.doctemplate.shared.dto.CommonRqDto;

@Component
@RequiredArgsConstructor
public class DocumentListUseCaseImpl implements DocumentListUseCase {
    private final DocumentQueryService documentQueryService;

    @Override
    public PageResult<Document> execute(UUID tenantId, UUID entityId, UUID objectId, CommonRqDto request) {
        return PageResult.<Document>builder()
            .data(documentQueryService.findAllByObject(tenantId, entityId, objectId, request))
            .totalRecordsAmount(documentQueryService.countByObject(tenantId, entityId, objectId, request))
            .build();
    }
}
