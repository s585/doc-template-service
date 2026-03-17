package ru.sberbank.sbercrm.doctemplate.template.usecase;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.sberbank.sbercrm.doctemplate.common.CommonRqDto;
import ru.sberbank.sbercrm.doctemplate.common.model.PageResult;
import ru.sberbank.sbercrm.doctemplate.template.model.Template;
import ru.sberbank.sbercrm.doctemplate.template.service.TemplateService;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ListTemplatesUseCaseImpl implements ListTemplatesUseCase {
    private final TemplateService templateService;

    @Override
    public PageResult<Template> execute(UUID tenantId, CommonRqDto request) {
        return templateService.findAll(tenantId, request);
    }
}
