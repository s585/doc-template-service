package ru.sberbank.sbercrm.saas.doctemplate.template.adapter;

import org.springframework.web.multipart.MultipartFile;
import ru.sberbank.sbercrm.saas.doctemplate.shared.dto.CommonRqDto;
import ru.sberbank.sbercrm.saas.doctemplate.shared.dto.CommonRsDto;
import ru.sberbank.sbercrm.saas.doctemplate.template.dto.TemplateCreationRq;
import ru.sberbank.sbercrm.saas.doctemplate.template.dto.TemplateRs;
import ru.sberbank.sbercrm.saas.doctemplate.template.dto.TemplateUpdateRq;

import java.util.UUID;

public interface TemplateWebAdapter {
    TemplateRs importTemplate(UUID tenantId, UUID userId, TemplateCreationRq request, MultipartFile file);

    TemplateRs updateTemplate(UUID tenantId, UUID userId, UUID templateId, TemplateUpdateRq request);

    TemplateRs getTemplate(UUID tenantId, UUID templateId);

    void deleteTemplate(UUID tenantId, UUID userId, UUID templateId);

    CommonRsDto listTemplates(UUID tenantId, CommonRqDto request);
}
