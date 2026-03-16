package ru.sberbank.sbercrm.doctemplate.template.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.sberbank.sbercrm.doctemplate.common.constant.CrmErrorCodes;
import ru.sberbank.sbercrm.doctemplate.common.exception.BusinessCrmException;
import ru.sberbank.sbercrm.doctemplate.template.model.Template;
import ru.sberbank.sbercrm.doctemplate.template.model.TemplateMapping;
import ru.sberbank.sbercrm.doctemplate.template.repository.TemplateMappingRepository;
import ru.sberbank.sbercrm.doctemplate.template.repository.TemplateRepository;

import java.util.List;
import java.util.UUID;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TemplateServiceImpl implements TemplateService {
    private final TemplateRepository templateRepository;
    private final TemplateMappingRepository templateMappingRepository;

    @Override
    public void checkCodeUnique(UUID tenantId, String code, UUID excludedTemplateId) {
        if (templateRepository.existsByCode(tenantId, code, excludedTemplateId)) {
            throw new BusinessCrmException(CrmErrorCodes.TEMPLATE_CODE_EXISTS, code);
        }
    }

    @Override
    public Template create(UUID tenantId, Template template) {
        return templateRepository.create(tenantId, template);
    }

    @Override
    public Template update(UUID tenantId, Template template) {
        return templateRepository.update(tenantId, template);
    }

    @Override
    public Optional<Template> findById(UUID tenantId, UUID templateId) {
        return templateRepository.findById(tenantId, templateId);
    }

    @Override
    public Optional<Template> findAggregateById(UUID tenantId, UUID templateId) {
        return templateRepository.findById(tenantId, templateId);
    }

    @Override
    public void createMappings(UUID tenantId, UUID templateId, UUID userId, List<TemplateMapping> mappings) {
        templateMappingRepository.createAll(tenantId, templateId, userId, mappings);
    }

    @Override
    public void replaceMappings(UUID tenantId, UUID templateId, UUID userId, List<TemplateMapping> mappings) {
        templateMappingRepository.deleteByTemplateId(tenantId, templateId);
        templateMappingRepository.createAll(tenantId, templateId, userId, mappings);
    }

    @Override
    public List<TemplateMapping> getMappings(UUID tenantId, UUID templateId) {
        return templateMappingRepository.findByTemplateId(tenantId, templateId);
    }
}
