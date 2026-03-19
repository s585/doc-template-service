package ru.sberbank.sbercrm.saas.doctemplate.template.usecase;

import ru.sberbank.sbercrm.doctemplate.shared.dto.CommonRqDto;
import ru.sberbank.sbercrm.saas.doctemplate.application.pagination.PageResult;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.Template;

import java.util.UUID;

public interface ListTemplatesUseCase {
    PageResult<Template> execute(UUID tenantId, CommonRqDto request);
}
