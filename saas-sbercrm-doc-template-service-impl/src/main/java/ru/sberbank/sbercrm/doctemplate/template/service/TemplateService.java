package ru.sberbank.sbercrm.doctemplate.template.service;

import ru.sberbank.sbercrm.doctemplate.template.model.Template;
import ru.sberbank.sbercrm.doctemplate.template.model.TemplateMapping;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TemplateService {
    void checkCodeUnique(UUID tenantId, String code, UUID excludedTemplateId);

    Template create(UUID tenantId, Template template);

    Template update(UUID tenantId, Template template);

    Optional<Template> findById(UUID tenantId, UUID templateId);

    Optional<Template> findAggregateById(UUID tenantId, UUID templateId);

    void createMappings(UUID tenantId, UUID templateId, UUID userId, List<TemplateMapping> mappings);

    void replaceMappings(UUID tenantId, UUID templateId, UUID userId, List<TemplateMapping> mappings);

    List<TemplateMapping> getMappings(UUID tenantId, UUID templateId);
}
