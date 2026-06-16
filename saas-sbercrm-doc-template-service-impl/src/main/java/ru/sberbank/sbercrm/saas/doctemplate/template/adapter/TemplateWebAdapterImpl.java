package ru.sberbank.sbercrm.saas.doctemplate.template.adapter;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import ru.sberbank.sbercrm.saas.doctemplate.application.pagination.CommonPageResponseBuilder;
import ru.sberbank.sbercrm.saas.doctemplate.shared.dto.CommonRqDto;
import ru.sberbank.sbercrm.saas.doctemplate.shared.dto.CommonRsDto;
import ru.sberbank.sbercrm.saas.doctemplate.application.pagination.PageResult;
import ru.sberbank.sbercrm.saas.doctemplate.template.dto.TemplateCreationRq;
import ru.sberbank.sbercrm.saas.doctemplate.template.dto.TemplateRs;
import ru.sberbank.sbercrm.saas.doctemplate.template.dto.TemplateUpdateRq;
import ru.sberbank.sbercrm.saas.doctemplate.template.converter.TemplateConverter;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.Template;
import ru.sberbank.sbercrm.saas.doctemplate.template.usecase.TemplateAvailableListingUseCase;
import ru.sberbank.sbercrm.saas.doctemplate.template.usecase.TemplateDeletionUseCase;
import ru.sberbank.sbercrm.saas.doctemplate.template.usecase.TemplateGetUseCase;
import ru.sberbank.sbercrm.saas.doctemplate.template.usecase.TemplateImportUseCase;
import ru.sberbank.sbercrm.saas.doctemplate.template.usecase.TemplateListingUseCase;
import ru.sberbank.sbercrm.saas.doctemplate.template.usecase.TemplateUpdateUseCase;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class TemplateWebAdapterImpl implements TemplateWebAdapter {
    private final TemplateConverter templateConverter;
    private final TemplateImportUseCase importTemplateUseCase;
    private final TemplateUpdateUseCase updateTemplateUseCase;
    private final TemplateGetUseCase getTemplateUseCase;
    private final TemplateDeletionUseCase deleteTemplateUseCase;
    private final TemplateListingUseCase listTemplatesUseCase;
    private final TemplateAvailableListingUseCase listAvailableTemplatesUseCase;

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
    public TemplateRs getTemplate(UUID tenantId, UUID templateId) {
        return templateConverter.convertToRs(getTemplateUseCase.execute(tenantId, templateId));
    }

    @Override
    public void deleteTemplate(UUID tenantId, UUID userId, UUID templateId) {
        deleteTemplateUseCase.execute(tenantId, userId, templateId);
    }

    @Override
    public CommonRsDto listTemplates(UUID tenantId, CommonRqDto request) {
        return CommonPageResponseBuilder.build(
            request,
            listTemplatesUseCase.execute(tenantId, request),
            templateConverter::convertToRs
        );
    }

    @Override
    public List<TemplateRs> listAvailableTemplates(UUID tenantId, UUID userId, UUID entityId, UUID objectId) {
        return listAvailableTemplatesUseCase.execute(tenantId, userId, entityId, objectId).stream()
            .map(templateConverter::convertToRs)
            .toList();
    }
}
