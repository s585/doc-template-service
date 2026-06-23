package ru.sberbank.sbercrm.saas.doctemplate.template.usecase;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.sberbank.sbercrm.saas.doctemplate.template.constant.TemplateConstants;
import ru.sberbank.sbercrm.saas.doctemplate.application.exception.model.NotFoundCrmException;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.Template;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.TemplateMapping;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.TemplateMappingLayout;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.TemplateUpdateCmd;
import ru.sberbank.sbercrm.saas.doctemplate.template.service.TemplateService;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class TemplateUpdateUseCaseImpl implements TemplateUpdateUseCase {
    private final TemplateService templateService;

    @Override
    @Transactional
    public Template execute(UUID tenantId, UUID userId, UUID templateId, TemplateUpdateCmd request) {
        Template currentTemplate = templateService.findById(tenantId, templateId)
            .orElseThrow(() -> new NotFoundCrmException(
                TemplateConstants.ErrorCodes.TEMPLATE_NOT_FOUND,
                TemplateConstants.ErrorCodes.TEMPLATE_NOT_FOUND,
                templateId
            ));

        Template updatedTemplate = templateService.update(
            tenantId,
            currentTemplate.toBuilder()
                .name(request.getName())
                .description(request.getDescription())
                .displayCondition(request.getDisplayCondition())
                .active(request.isActive())
                .updatedBy(userId)
                .build()
        );

        templateService.replaceMappings(
            tenantId,
            templateId,
            userId,
            preserveReadOnlyLayouts(request.getMappings(), currentTemplate.getMappings())
        );
        updatedTemplate.setMappings(templateService.getMappings(tenantId, templateId));
        return updatedTemplate;
    }

    private List<TemplateMapping> preserveReadOnlyLayouts(
        List<TemplateMapping> mappings,
        List<TemplateMapping> currentMappings
    ) {
        if (currentMappings == null || currentMappings.isEmpty()) {
            return mappings;
        }

        Map<String, TemplateMappingLayout> keyToLayout = currentMappings.stream()
            .filter(mapping -> mapping.getKey() != null)
            .filter(mapping -> mapping.getDefinition() != null)
            .filter(mapping -> mapping.getDefinition().getLayout() != null)
            .collect(Collectors.toMap(
                TemplateMapping::getKey,
                mapping -> mapping.getDefinition().getLayout(),
                (left, right) -> left
            ));

        return mappings.stream()
            .map(mapping -> preserveReadOnlyLayout(mapping, keyToLayout.get(mapping.getKey())))
            .toList();
    }

    private TemplateMapping preserveReadOnlyLayout(TemplateMapping mapping, TemplateMappingLayout layout) {
        if (layout == null || mapping.getDefinition() == null) {
            return mapping;
        }

        return mapping.toBuilder()
            .definition(mapping.getDefinition().toBuilder()
                .layout(layout)
                .build())
            .build();
    }
}
