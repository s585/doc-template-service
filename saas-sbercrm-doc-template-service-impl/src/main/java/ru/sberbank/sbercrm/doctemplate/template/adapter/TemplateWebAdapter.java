package ru.sberbank.sbercrm.doctemplate.template.adapter;

import org.springframework.web.multipart.MultipartFile;
import ru.sberbank.sbercrm.doctemplate.template.TemplateCreationRq;
import ru.sberbank.sbercrm.doctemplate.template.TemplateRs;

import java.util.UUID;

public interface TemplateWebAdapter {
    TemplateRs importTemplate(UUID tenantId, UUID userId, TemplateCreationRq request, MultipartFile file);
}
