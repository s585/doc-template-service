package ru.sberbank.sbercrm.saas.doctemplate.template.usecase;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.sberbank.sbercrm.saas.doctemplate.shared.dto.CommonRqDto;
import ru.sberbank.sbercrm.saas.doctemplate.application.pagination.PageResult;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.Template;
import ru.sberbank.sbercrm.saas.doctemplate.template.service.TemplateService;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class TemplateListingUseCaseImpl implements TemplateListingUseCase {
    private final TemplateService templateService;

    @Override
    public PageResult<Template> execute(UUID tenantId, CommonRqDto request) {
        return templateService.findAll(tenantId, request);
    }
}
