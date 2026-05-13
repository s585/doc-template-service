package ru.sberbank.sbercrm.saas.doctemplate.document.service.context;

import java.util.Map;
import org.springframework.stereotype.Component;
import ru.sberbank.sbercrm.saas.doctemplate.application.exception.model.BusinessCrmException;
import ru.sberbank.sbercrm.saas.doctemplate.document.constant.DocumentConstants;

@Component
public class GenerationPathResolver {
    private static final String SOURCE_PREFIX = "source.";
    private static final String REFERENCE_PREFIX = "reference.";

    public Object resolveSourcePath(Map<String, Object> sourceObject, String rawPath, String mappingKey) {
        if (sourceObject == null) {
            return null;
        }
        return resolvePath(sourceObject, rawPath, SOURCE_PREFIX, mappingKey);
    }

    public Object resolveReferencePath(Map<String, Object> referenceObject, String rawPath, String mappingKey) {
        if (referenceObject == null) {
            return null;
        }
        return resolvePath(referenceObject, rawPath, REFERENCE_PREFIX, mappingKey);
    }

    public String normalizeSourcePath(String rawPath, String mappingKey) {
        return normalizePath(rawPath, SOURCE_PREFIX, mappingKey);
    }

    private Object resolvePath(Map<String, Object> sourceObject, String rawPath, String expectedPrefix, String mappingKey) {
        String normalizedPath = normalizePath(rawPath, expectedPrefix, mappingKey);
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

    private String normalizePath(String rawPath, String expectedPrefix, String mappingKey) {
        String trimmedPath = rawPath == null ? "" : rawPath.trim();
        if (!trimmedPath.startsWith(expectedPrefix)) {
            throw invalidBusinessObjectPath(mappingKey, rawPath);
        }
        String normalizedPath = trimmedPath.substring(expectedPrefix.length());
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
