package ru.sberbank.sbercrm.doctemplate.template.adapter;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import ru.sberbank.sbercrm.doctemplate.common.CommonRqDto;
import ru.sberbank.sbercrm.doctemplate.common.CommonRsDto;
import ru.sberbank.sbercrm.doctemplate.common.PagingRsDto;
import ru.sberbank.sbercrm.doctemplate.common.model.PageResult;
import ru.sberbank.sbercrm.doctemplate.template.TemplateCreationRq;
import ru.sberbank.sbercrm.doctemplate.template.TemplateRs;
import ru.sberbank.sbercrm.doctemplate.template.TemplateUpdateRq;
import ru.sberbank.sbercrm.doctemplate.template.converter.TemplateConverter;
import ru.sberbank.sbercrm.doctemplate.template.model.Template;
import ru.sberbank.sbercrm.doctemplate.template.usecase.DeleteTemplateUseCase;
import ru.sberbank.sbercrm.doctemplate.template.usecase.ImportTemplateUseCase;
import ru.sberbank.sbercrm.doctemplate.template.usecase.ListTemplatesUseCase;
import ru.sberbank.sbercrm.doctemplate.template.usecase.UpdateTemplateUseCase;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class TemplateWebAdapterImpl implements TemplateWebAdapter {
    private final TemplateConverter templateConverter;
    private final ImportTemplateUseCase importTemplateUseCase;
    private final UpdateTemplateUseCase updateTemplateUseCase;
    private final DeleteTemplateUseCase deleteTemplateUseCase;
    private final ListTemplatesUseCase listTemplatesUseCase;

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

    @Override
    public CommonRsDto listTemplates(UUID tenantId, CommonRqDto request) {
        PageResult<Template> result = listTemplatesUseCase.execute(tenantId, request);
        long pageSize = request.getPaging().getSize();

        return CommonRsDto.builder()
            .data(result.getData().stream().map(templateConverter::convertToRs).toList())
            .paging(
                PagingRsDto.builder()
                    .currentPage(request.getPaging().getPage().longValue())
                    .recordsOnPage((long) result.getData().size())
                    .totalRecordsAmount(result.getTotalRecordsAmount())
                    .totalPageAmount(pageSize == 0 ? 0L : (result.getTotalRecordsAmount() + pageSize - 1) / pageSize)
                    .build()
            )
            .build();
    }
}
