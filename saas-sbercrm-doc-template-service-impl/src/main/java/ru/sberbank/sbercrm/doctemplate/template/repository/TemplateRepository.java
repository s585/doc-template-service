package ru.sberbank.sbercrm.doctemplate.template.repository;

import ru.sberbank.sbercrm.doctemplate.common.CommonRqDto;
import ru.sberbank.sbercrm.doctemplate.template.model.Template;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TemplateRepository {
    Template create(UUID tenantId, Template template);

    Template update(UUID tenantId, Template template);

    Optional<Template> findById(UUID tenantId, UUID templateId);

    List<Template> findAll(UUID tenantId, CommonRqDto request);

    List<Template> findAllByEntityId(UUID tenantId, UUID entityId);

    boolean existsByCode(UUID tenantId, String code, UUID excludedTemplateId);

    void deleteById(UUID tenantId, UUID templateId);
}
