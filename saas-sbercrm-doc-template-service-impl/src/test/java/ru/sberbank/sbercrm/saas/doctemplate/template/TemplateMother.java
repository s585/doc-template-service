package ru.sberbank.sbercrm.saas.doctemplate.template;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.test.context.TestComponent;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.Template;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.TemplateFormat;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.TemplateMapping;
import ru.sberbank.sbercrm.saas.doctemplate.template.service.TemplateService;

import java.util.List;
import java.util.UUID;

@TestComponent
@RequiredArgsConstructor
public class TemplateMother {
    private final TemplateService templateService;

    public Template createTemplate(
        UUID tenantId,
        UUID userId,
        UUID entityId,
        String name,
        String code,
        String description,
        TemplateFormat format,
        String s3Key,
        boolean active
    ) {
        return templateService.create(
            tenantId,
            Template.builder()
                .entityId(entityId)
                .name(name)
                .code(code)
                .description(description)
                .format(format)
                .s3Key(s3Key)
                .active(active)
                .createdBy(userId)
                .updatedBy(userId)
                .build()
        );
    }

    public Template createTemplateWithMappings(
        UUID tenantId,
        UUID userId,
        UUID entityId,
        String name,
        String code,
        String description,
        TemplateFormat format,
        String s3Key,
        boolean active,
        List<TemplateMapping> mappings
    ) {
        Template template = createTemplate(tenantId, userId, entityId, name, code, description, format, s3Key, active);
        templateService.createMappings(tenantId, template.getId(), userId, mappings);
        return template;
    }

    public Template findAggregateById(UUID tenantId, UUID templateId) {
        return templateService.findAggregateById(tenantId, templateId).orElseThrow();
    }

    public boolean exists(UUID tenantId, UUID templateId) {
        return templateService.findAggregateById(tenantId, templateId).isPresent();
    }
}
