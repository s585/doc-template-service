package ru.sberbank.sbercrm.doctemplate.template.repository;

import ru.sberbank.sbercrm.doctemplate.template.model.TemplateMapping;

import java.util.List;
import java.util.UUID;

public interface TemplateMappingRepository {
    List<TemplateMapping> findByTemplateId(UUID tenantId, UUID templateId);

    void createAll(UUID tenantId, UUID templateId, UUID userId, List<TemplateMapping> mappings);

    void deleteByTemplateId(UUID tenantId, UUID templateId);
}
