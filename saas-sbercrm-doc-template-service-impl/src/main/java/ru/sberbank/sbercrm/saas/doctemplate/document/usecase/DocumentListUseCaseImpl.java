package ru.sberbank.sbercrm.saas.doctemplate.document.usecase;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.sberbank.sbercrm.saas.doctemplate.application.pagination.CommonPageResponseBuilder;
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
        var documents = documentQueryService.findAllByObject(tenantId, entityId, objectId, request);
        long totalRecordsAmount = documentQueryService.countByObject(tenantId, entityId, objectId, request);
        return PageResult.<Document>builder()
            .data(documents)
            .paging(CommonPageResponseBuilder.buildPaging(request, documents.size(), totalRecordsAmount))
            .build();
    }
}
