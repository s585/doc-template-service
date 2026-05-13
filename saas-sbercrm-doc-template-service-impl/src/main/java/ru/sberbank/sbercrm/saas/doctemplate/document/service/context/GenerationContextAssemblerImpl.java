package ru.sberbank.sbercrm.saas.doctemplate.document.service.context;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.sberbank.sbercrm.saas.doctemplate.application.exception.model.BusinessCrmException;
import ru.sberbank.sbercrm.saas.doctemplate.application.integration.gateway.BusinessObjectGateway;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.CollectionDataset;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.CollectionQueryKey;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.GenerationJob;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.GenerationTemplateContext;
import ru.sberbank.sbercrm.saas.doctemplate.document.service.context.expression.ExpressionEvaluator;
import ru.sberbank.sbercrm.saas.doctemplate.shared.dto.SelectDto;
import ru.sberbank.sbercrm.saas.doctemplate.template.constant.TemplateConstants;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.MappingScope;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.Template;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.TemplateMapping;

@Service
@RequiredArgsConstructor
public class GenerationContextAssemblerImpl implements GenerationContextAssembler {
    private final BusinessObjectGateway businessObjectGateway;
    private final GenerationSelectBuilder generationSelectBuilder;
    private final GenerationMappingPlanner generationMappingPlanner;
    private final GenerationSourceValueResolver sourceValueResolver;
    private final GenerationCollectionDatasetResolver collectionDatasetResolver;
    private final ExpressionEvaluator expressionEvaluator;

    @Override
    public GenerationTemplateContext assemble(GenerationJob job, UUID userId, Template template) {
        Map<String, Object> sourceObject = resolveSourceObject(job, userId, template);
        Map<String, String> scalarValues = new HashMap<>();
        List<CollectionDataset> collections = new ArrayList<>();
        String generatedFileBaseName = null;

        List<TemplateMapping> mappings = template.getMappings() == null ? List.of() : template.getMappings();
        GenerationMappingPlan plan = generationMappingPlanner.build(mappings);

        for (TemplateMapping scalarMapping : plan.scalarMappings()) {
            resolveScalarValue(scalarMapping, sourceObject, job, userId, scalarValues);
        }
        for (TemplateMapping generatedFileNameMapping : plan.generatedFileNameMappings()) {
            generatedFileBaseName = resolveGeneratedFileName(
                generatedFileNameMapping,
                sourceObject,
                job,
                userId,
                generatedFileBaseName
            );
        }
        collections.addAll(resolveCollections(plan.collectionGroups(), sourceObject, job, userId));
        validateResolvedCollections(mappings, collections);

        if (generatedFileBaseName == null || generatedFileBaseName.isBlank()) {
            generatedFileBaseName = template.getName();
        }

        String extension = template.getFormat().value().toLowerCase();
        String generatedFileName = generatedFileBaseName.endsWith("." + extension)
            ? generatedFileBaseName
            : generatedFileBaseName + "." + extension;
        return GenerationTemplateContext.builder()
            .scalarValues(scalarValues)
            .collections(collections)
            .generatedFileName(generatedFileName)
            .build();
    }

    private void resolveScalarValue(
        TemplateMapping mapping,
        Map<String, Object> sourceObject,
        GenerationJob job,
        UUID userId,
        Map<String, String> scalarValues
    ) {
        scalarValues.put(mapping.getKey(), toStringValue(resolveEvaluatedValue(mapping, sourceObject, job, userId)));
    }

    private String resolveGeneratedFileName(
        TemplateMapping mapping,
        Map<String, Object> sourceObject,
        GenerationJob job,
        UUID userId,
        String currentBaseName
    ) {
        if (currentBaseName != null && !currentBaseName.isBlank()) {
            return currentBaseName;
        }
        String resolvedValue = toStringValue(resolveEvaluatedValue(mapping, sourceObject, job, userId));
        return resolvedValue.isBlank() ? currentBaseName : resolvedValue;
    }

    private Object resolveEvaluatedValue(
        TemplateMapping mapping,
        Map<String, Object> sourceObject,
        GenerationJob job,
        UUID userId
    ) {
        Object sourceValue = sourceValueResolver.resolve(mapping, sourceObject, job.getTenantId(), userId);
        return expressionEvaluator.evaluate(mapping, sourceValue);
    }

    private List<CollectionDataset> resolveCollections(
        Map<CollectionQueryKey, List<TemplateMapping>> collectionGroups,
        Map<String, Object> sourceObject,
        GenerationJob job,
        UUID userId
    ) {
        List<CollectionDataset> datasets = new ArrayList<>();
        for (List<TemplateMapping> groupMappings : collectionGroups.values()) {
            datasets.add(collectionDatasetResolver.resolve(groupMappings, sourceObject, job.getTenantId(), userId));
        }
        return datasets;
    }

    private void validateResolvedCollections(List<TemplateMapping> mappings, List<CollectionDataset> collections) {
        Set<String> declaredCollectionKeys = mappings.stream()
            .filter(mapping -> mapping.getDefinition() != null && mapping.getDefinition().getScope() == MappingScope.COLLECTION)
            .map(TemplateMapping::getKey)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<String> resolvedCollectionKeys = collections.stream()
            .flatMap(dataset -> dataset.getKeys().stream())
            .collect(Collectors.toCollection(LinkedHashSet::new));
        List<String> missingKeys = declaredCollectionKeys.stream()
            .filter(key -> !resolvedCollectionKeys.contains(key))
            .toList();
        if (!missingKeys.isEmpty()) {
            throw new BusinessCrmException(
                TemplateConstants.ErrorCodes.TEMPLATE_COLLECTION_PLACEHOLDERS_MISSING_DATASET,
                TemplateConstants.ErrorCodes.TEMPLATE_COLLECTION_PLACEHOLDERS_MISSING_DATASET,
                missingKeys.toString()
            );
        }

        Map<String, Integer> keyUsage = new LinkedHashMap<>();
        for (CollectionDataset dataset : collections) {
            for (String key : dataset.getKeys()) {
                keyUsage.merge(key, 1, Integer::sum);
            }
        }
        List<String> ambiguousKeys = keyUsage.entrySet().stream()
            .filter(entry -> entry.getValue() > 1)
            .map(Map.Entry::getKey)
            .toList();
        if (!ambiguousKeys.isEmpty()) {
            throw new BusinessCrmException(
                TemplateConstants.ErrorCodes.TEMPLATE_COLLECTION_PLACEHOLDERS_AMBIGUOUS,
                TemplateConstants.ErrorCodes.TEMPLATE_COLLECTION_PLACEHOLDERS_AMBIGUOUS,
                ambiguousKeys.toString()
            );
        }
    }

    private Map<String, Object> resolveSourceObject(GenerationJob job, UUID userId, Template template) {
        SelectDto selectDto = generationSelectBuilder.build(template);
        if (selectDto.getFields() == null || selectDto.getFields().isEmpty()) {
            return null;
        }
        return businessObjectGateway.getObject(job.getTenantId(), userId, job.getEntityId(), job.getObjectId(), selectDto);
    }

    private String toStringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

}
