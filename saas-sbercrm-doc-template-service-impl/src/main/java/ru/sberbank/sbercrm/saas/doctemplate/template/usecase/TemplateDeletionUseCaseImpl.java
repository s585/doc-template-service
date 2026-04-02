package ru.sberbank.sbercrm.saas.doctemplate.template.usecase;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.sberbank.sbercrm.saas.doctemplate.template.constant.TemplateConstants;
import ru.sberbank.sbercrm.saas.doctemplate.application.exception.model.NotFoundCrmException;
import ru.sberbank.sbercrm.saas.doctemplate.application.integration.gateway.FileStorageGateway;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.Template;
import ru.sberbank.sbercrm.saas.doctemplate.template.service.TemplateService;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class TemplateDeletionUseCaseImpl implements TemplateDeletionUseCase {
    private final TemplateService templateService;
    private final FileStorageGateway fileStorageGateway;

    @Override
    @Transactional
    public void execute(UUID tenantId, UUID userId, UUID templateId) {
        Template template = templateService.findById(tenantId, templateId)
            .orElseThrow(() -> new NotFoundCrmException(TemplateConstants.ErrorCodes.TEMPLATE_NOT_FOUND, templateId));

        fileStorageGateway.deleteFile(tenantId, userId, template.getS3Key());
        templateService.delete(tenantId, templateId);
    }
}
