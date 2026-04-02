package ru.sberbank.sbercrm.saas.doctemplate.document.usecase;

import java.util.UUID;
import ru.sberbank.sbercrm.saas.doctemplate.application.pagination.PageResult;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.Document;
import ru.sberbank.sbercrm.saas.doctemplate.shared.dto.CommonRqDto;

public interface DocumentListUseCase {
    PageResult<Document> execute(UUID tenantId, UUID entityId, UUID objectId, CommonRqDto request);
}
