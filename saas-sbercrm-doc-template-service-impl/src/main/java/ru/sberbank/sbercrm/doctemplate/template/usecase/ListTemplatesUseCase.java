package ru.sberbank.sbercrm.doctemplate.template.usecase;

import ru.sberbank.sbercrm.doctemplate.common.CommonRqDto;
import ru.sberbank.sbercrm.doctemplate.common.model.PageResult;
import ru.sberbank.sbercrm.doctemplate.template.model.Template;

import java.util.UUID;

public interface ListTemplatesUseCase {
    PageResult<Template> execute(UUID tenantId, CommonRqDto request);
}
