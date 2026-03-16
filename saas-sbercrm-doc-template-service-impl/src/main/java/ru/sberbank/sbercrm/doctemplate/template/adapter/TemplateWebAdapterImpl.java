package ru.sberbank.sbercrm.doctemplate.template.adapter;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import ru.sberbank.sbercrm.doctemplate.template.TemplateCreationRq;
import ru.sberbank.sbercrm.doctemplate.template.TemplateRs;
import ru.sberbank.sbercrm.doctemplate.template.TemplateUpdateRq;
import ru.sberbank.sbercrm.doctemplate.template.converter.TemplateConverter;
import ru.sberbank.sbercrm.doctemplate.template.model.Template;
import ru.sberbank.sbercrm.doctemplate.template.usecase.DeleteTemplateUseCase;
import ru.sberbank.sbercrm.doctemplate.template.usecase.ImportTemplateUseCase;
import ru.sberbank.sbercrm.doctemplate.template.usecase.UpdateTemplateUseCase;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class TemplateWebAdapterImpl implements TemplateWebAdapter {
    private final TemplateConverter templateConverter;
    private final ImportTemplateUseCase importTemplateUseCase;
    private final UpdateTemplateUseCase updateTemplateUseCase;
    private final DeleteTemplateUseCase deleteTemplateUseCase;

    @Override
    public TemplateRs importTemplate(UUID tenantId, UUID userId, TemplateCreationRq request, MultipartFile file) {
        Template template = importTemplateUseCase.execute(
            tenantId,
            userId,
            templateConverter.convertToModel(request),
            file
        );
        return templateConverter.convertToRs(template);
    }

    @Override
    public TemplateRs updateTemplate(UUID tenantId, UUID userId, UUID templateId, TemplateUpdateRq request) {
        Template template = updateTemplateUseCase.execute(
            tenantId,
            userId,
            templateId,
            templateConverter.convertToModel(request)
        );
        return templateConverter.convertToRs(template);
    }

    @Override
    public void deleteTemplate(UUID tenantId, UUID userId, UUID templateId) {
        deleteTemplateUseCase.execute(tenantId, userId, templateId);
    }
}
