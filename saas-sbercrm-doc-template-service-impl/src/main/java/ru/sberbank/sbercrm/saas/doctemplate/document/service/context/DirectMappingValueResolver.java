package ru.sberbank.sbercrm.saas.doctemplate.document.service.context;

import java.util.Map;
import org.springframework.stereotype.Component;
import ru.sberbank.sbercrm.saas.doctemplate.application.exception.model.BusinessCrmException;
import ru.sberbank.sbercrm.saas.doctemplate.document.constant.DocumentConstants;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.TemplateMapping;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.source.DirectValueSource;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.source.ValueSource;

@Component
public class DirectMappingValueResolver implements MappingValueResolver {
    @Override
    public boolean supports(ValueSource source) {
        return source instanceof DirectValueSource;
    }

    @Override
    public Object resolve(TemplateMapping mapping, ValueSource source, Map<String, Object> sourceObject) {
        DirectValueSource directSource = (DirectValueSource) source;
        if (sourceObject == null) {
            return "";
        }
        if (directSource.getPath() == null || directSource.getPath().isBlank()) {
            throw invalidBusinessObjectPath(mapping.getKey(), directSource.getPath());
        }
        Object resolvedValue = resolvePath(sourceObject, directSource.getPath(), mapping.getKey());
        return resolvedValue == null ? "" : resolvedValue;
    }

    private Object resolvePath(Map<String, Object> sourceObject, String rawPath, String mappingKey) {
        String normalizedPath = normalizeSourcePath(rawPath, mappingKey);
        Object currentNode = sourceObject;
        for (String segment : normalizedPath.split("\\.")) {
            if (segment.isBlank() || !(currentNode instanceof Map<?, ?> currentMap)) {
                throw invalidBusinessObjectPath(mappingKey, rawPath);
            }
            if (!currentMap.containsKey(segment)) {
                throw invalidBusinessObjectPath(mappingKey, rawPath);
            }
            currentNode = currentMap.get(segment);
        }
        return currentNode;
    }

    private String normalizeSourcePath(String rawPath, String mappingKey) {
        String trimmedPath = rawPath == null ? "" : rawPath.trim();
        if (!trimmedPath.startsWith("source.")) {
            throw invalidBusinessObjectPath(mappingKey, rawPath);
        }
        String normalizedPath = trimmedPath.substring("source.".length());
        if (normalizedPath.isBlank()) {
            throw invalidBusinessObjectPath(mappingKey, rawPath);
        }
        return normalizedPath;
    }

    private BusinessCrmException invalidBusinessObjectPath(String mappingKey, String rawPath) {
        return new BusinessCrmException(
            DocumentConstants.ErrorCodes.GENERATION_BUSINESS_OBJECT_PATH_INVALID,
            DocumentConstants.ErrorCodes.GENERATION_BUSINESS_OBJECT_PATH_INVALID,
            mappingKey,
            rawPath
        );
    }
}
