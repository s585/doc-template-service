package ru.sberbank.sbercrm.doctemplate.template.usecase;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.sberbank.sbercrm.doctemplate.common.constant.CrmErrorCodes;
import ru.sberbank.sbercrm.doctemplate.common.exception.NotFoundCrmException;
import ru.sberbank.sbercrm.doctemplate.template.adapter.filestorage.FileStorageAdapter;
import ru.sberbank.sbercrm.doctemplate.template.model.Template;
import ru.sberbank.sbercrm.doctemplate.template.service.TemplateService;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class DeleteTemplateUseCaseImpl implements DeleteTemplateUseCase {
    private final TemplateService templateService;
    private final FileStorageAdapter fileStorageAdapter;

    @Override
    @Transactional
    public void execute(UUID tenantId, UUID userId, UUID templateId) {
        Template template = templateService.findById(tenantId, templateId)
            .orElseThrow(() -> new NotFoundCrmException(CrmErrorCodes.TEMPLATE_NOT_FOUND, templateId));

        fileStorageAdapter.deleteFile(tenantId, userId, template.getS3Key());
        templateService.delete(tenantId, templateId);
    }
}
