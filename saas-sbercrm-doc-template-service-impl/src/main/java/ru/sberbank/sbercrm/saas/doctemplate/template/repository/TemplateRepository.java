package ru.sberbank.sbercrm.saas.doctemplate.template.repository;

import ru.sberbank.sbercrm.saas.doctemplate.shared.dto.CommonRqDto;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.Template;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TemplateRepository {
    Template create(UUID tenantId, Template template);

    Template update(UUID tenantId, Template template);

    Optional<Template> findById(UUID tenantId, UUID templateId);

    boolean exists(UUID tenantId, UUID templateId);

    List<Template> findAll(UUID tenantId, CommonRqDto request);

    long count(UUID tenantId, CommonRqDto request);

    List<Template> findAllByEntityId(UUID tenantId, UUID entityId);

    List<Template> findAllActiveByEntityIdOrderByNameAndId(UUID tenantId, UUID entityId);

    boolean existsByCode(UUID tenantId, String code, UUID excludedTemplateId);

    void deleteById(UUID tenantId, UUID templateId);
}
