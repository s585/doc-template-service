package ru.sberbank.sbercrm.saas.doctemplate.template.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.sberbank.sbercrm.saas.doctemplate.shared.dto.CommonRqDto;
import ru.sberbank.sbercrm.saas.doctemplate.template.constant.TemplateConstants;
import ru.sberbank.sbercrm.saas.doctemplate.application.exception.model.BusinessCrmException;
import ru.sberbank.sbercrm.saas.doctemplate.application.pagination.PageResult;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.Template;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.TemplateMapping;
import ru.sberbank.sbercrm.saas.doctemplate.template.repository.TemplateMappingRepository;
import ru.sberbank.sbercrm.saas.doctemplate.template.repository.TemplateRepository;

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
            throw new BusinessCrmException(
                TemplateConstants.ErrorCodes.TEMPLATE_CODE_EXISTS,
                TemplateConstants.ErrorCodes.TEMPLATE_CODE_EXISTS,
                code
            );
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
    @Transactional
    public void delete(UUID tenantId, UUID templateId) {
        templateMappingRepository.deleteByTemplateId(tenantId, templateId);
        templateRepository.deleteById(tenantId, templateId);
    }

    @Override
    public Optional<Template> findById(UUID tenantId, UUID templateId) {
        return findAggregateById(tenantId, templateId);
    }

    @Override
    public boolean exists(UUID tenantId, UUID templateId) {
        return templateRepository.exists(tenantId, templateId);
    }

    @Override
    public Optional<Template> findAggregateById(UUID tenantId, UUID templateId) {
        return templateRepository.findById(tenantId, templateId);
    }

    @Override
    public PageResult<Template> findAll(UUID tenantId, CommonRqDto request) {
        return PageResult.<Template>builder()
            .data(templateRepository.findAll(tenantId, request))
            .totalRecordsAmount(templateRepository.count(tenantId, request))
            .build();
    }

    @Override
    public void createMappings(UUID tenantId, UUID templateId, UUID userId, List<TemplateMapping> mappings) {
        templateMappingRepository.createAll(tenantId, templateId, userId, mappings);
    }

    @Override
    @Transactional
    public void replaceMappings(UUID tenantId, UUID templateId, UUID userId, List<TemplateMapping> mappings) {
        templateMappingRepository.deleteByTemplateId(tenantId, templateId);
        templateMappingRepository.createAll(tenantId, templateId, userId, mappings);
    }

    @Override
    public List<TemplateMapping> getMappings(UUID tenantId, UUID templateId) {
        return templateMappingRepository.findByTemplateId(tenantId, templateId);
    }
}
