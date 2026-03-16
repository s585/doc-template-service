package ru.sberbank.sbercrm.doctemplate.template.adapter;

import org.springframework.web.multipart.MultipartFile;
import ru.sberbank.sbercrm.doctemplate.template.TemplateCreationRq;
import ru.sberbank.sbercrm.doctemplate.template.TemplateRs;
import ru.sberbank.sbercrm.doctemplate.template.TemplateUpdateRq;

import java.util.UUID;

public interface TemplateWebAdapter {
    TemplateRs importTemplate(UUID tenantId, UUID userId, TemplateCreationRq request, MultipartFile file);

    TemplateRs updateTemplate(UUID tenantId, UUID userId, UUID templateId, TemplateUpdateRq request);

    void deleteTemplate(UUID tenantId, UUID userId, UUID templateId);
}
