package ru.sberbank.sbercrm.saas.doctemplate.document.service.context;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.sberbank.sbercrm.saas.doctemplate.application.integration.gateway.BusinessObjectGateway;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.GenerationJob;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.GenerationTemplateContext;
import ru.sberbank.sbercrm.saas.doctemplate.document.service.context.expression.ExpressionEvaluator;
import ru.sberbank.sbercrm.saas.doctemplate.template.constant.TemplateConstants;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.Template;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.TemplateMapping;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.source.DirectValueSource;

@Service
@RequiredArgsConstructor
public class GenerationContextAssemblerImpl implements GenerationContextAssembler {
    private final GenerationSourceValueResolver sourceValueResolver;
    private final BusinessObjectGateway businessObjectGateway;
    private final ExpressionEvaluator expressionEvaluator;

    @Override
    public GenerationTemplateContext assemble(GenerationJob job, UUID userId, Template template) {
        Map<String, Object> sourceObject = resolveSourceObject(job, userId, template);
        Map<String, String> values = new HashMap<>();
        String baseName = null;

        if (template.getMappings() != null) {
            for (TemplateMapping mapping : template.getMappings()) {
                Object sourceValue = sourceValueResolver.resolve(mapping, sourceObject);
                Object evaluatedValue = expressionEvaluator.evaluate(mapping, sourceValue);
                String resolvedValue = toStringValue(evaluatedValue);
                if (TemplateConstants.MappingKeys.GENERATED_FILE_NAME.equals(mapping.getKey())) {
                    if (baseName == null && !resolvedValue.isBlank()) {
                        baseName = resolvedValue;
                    }
                    continue;
                }
                values.put(mapping.getKey(), resolvedValue);
            }
        }

        if (baseName == null || baseName.isBlank()) {
            baseName = template.getName();
        }

        String extension = template.getFormat().value().toLowerCase();
        String generatedFileName = baseName.endsWith("." + extension) ? baseName : baseName + "." + extension;
        return GenerationTemplateContext.builder()
            .values(values)
            .generatedFileName(generatedFileName)
            .build();
    }

    private Map<String, Object> resolveSourceObject(GenerationJob job, UUID userId, Template template) {
        if (template.getMappings() == null) {
            return null;
        }
        boolean requiresSourceObject = template.getMappings().stream()
            .map(TemplateMapping::getDefinition)
            .anyMatch(definition -> definition != null && definition.getSource() instanceof DirectValueSource);
        if (!requiresSourceObject) {
            return null;
        }
        return businessObjectGateway.getObject(job.getTenantId(), userId, job.getEntityId(), job.getObjectId());
    }

    private String toStringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
