package ru.sberbank.sbercrm.saas.doctemplate.template.usecase;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.sberbank.sbercrm.saas.doctemplate.application.exception.model.NotFoundCrmException;
import ru.sberbank.sbercrm.saas.doctemplate.template.constant.TemplateConstants;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.Template;
import ru.sberbank.sbercrm.saas.doctemplate.template.service.TemplateService;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class TemplateGetUseCaseImpl implements TemplateGetUseCase {
    private final TemplateService templateService;

    @Override
    public Template execute(UUID tenantId, UUID templateId) {
        return templateService.findById(tenantId, templateId)
            .orElseThrow(() -> new NotFoundCrmException(
                TemplateConstants.ErrorCodes.TEMPLATE_NOT_FOUND,
                TemplateConstants.ErrorCodes.TEMPLATE_NOT_FOUND,
                templateId
            ));
    }
}
