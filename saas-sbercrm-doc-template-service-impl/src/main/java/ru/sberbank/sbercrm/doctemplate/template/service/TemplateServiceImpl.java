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
import java.util.Optional;
import java.util.UUID;

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
    public Template getAggregateById(UUID tenantId, UUID templateId) {
        return templateRepository.findById(tenantId, templateId)
                .orElseThrow(() -> new BusinessCrmException("No data found"));
    }

    @Override
    public void createMappings(UUID tenantId, UUID templateId, UUID userId, List<TemplateMapping> mappings) {
        templateMappingRepository.createAll(tenantId, templateId, userId, mappings);
    }

    @Override
    public List<TemplateMapping> getMappings(UUID tenantId, UUID templateId) {
        return templateMappingRepository.findByTemplateId(tenantId, templateId);
    }
}
