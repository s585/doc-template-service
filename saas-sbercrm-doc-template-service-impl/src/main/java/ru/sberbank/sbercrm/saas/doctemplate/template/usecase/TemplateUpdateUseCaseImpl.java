package ru.sberbank.sbercrm.saas.doctemplate.template.usecase;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.sberbank.sbercrm.saas.doctemplate.application.exception.CrmErrorCodes;
import ru.sberbank.sbercrm.saas.doctemplate.template.constant.TemplateConstants;
import ru.sberbank.sbercrm.saas.doctemplate.application.exception.model.NotFoundCrmException;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.Template;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.TemplateUpdateCmd;
import ru.sberbank.sbercrm.saas.doctemplate.template.service.TemplateService;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class TemplateUpdateUseCaseImpl implements TemplateUpdateUseCase {
    private final TemplateService templateService;

    @Override
    @Transactional
    public Template execute(UUID tenantId, UUID userId, UUID templateId, TemplateUpdateCmd request) {
        Template currentTemplate = templateService.findById(tenantId, templateId)
            .orElseThrow(() -> new NotFoundCrmException(TemplateConstants.ErrorCodes.TEMPLATE_NOT_FOUND, templateId));

        Template updatedTemplate = templateService.update(
            tenantId,
            currentTemplate.toBuilder()
                .name(request.getName())
                .description(request.getDescription())
                .displayCondition(request.getDisplayCondition())
                .active(request.isActive())
                .updatedBy(userId)
                .build()
        );

        templateService.replaceMappings(tenantId, templateId, userId, request.getMappings());
        updatedTemplate.setMappings(templateService.getMappings(tenantId, templateId));
        return updatedTemplate;
    }
}
