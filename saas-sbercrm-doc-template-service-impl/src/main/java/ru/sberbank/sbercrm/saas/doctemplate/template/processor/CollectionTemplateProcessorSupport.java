package ru.sberbank.sbercrm.saas.doctemplate.template.processor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import ru.sberbank.sbercrm.saas.doctemplate.application.exception.model.BusinessCrmException;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.CollectionDataset;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.GenerationTemplateContext;
import ru.sberbank.sbercrm.saas.doctemplate.template.constant.TemplateConstants;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class CollectionTemplateProcessorSupport {

    static CollectionDataset resolveCollectionDataset(Set<String> placeholders, GenerationTemplateContext context) {
        // Шаблонная строка может смешивать скалярные placeholder-ы и placeholder-ы коллекции.
        // Dataset выбираем только по collection keys, а скалярные значения потом добавляются в каждую строку.
        List<String> collectionKeys = placeholders.stream()
            .filter(CollectionTemplateProcessorSupport::hasText)
            .filter(key -> context.getCollections().stream().anyMatch(dataset -> dataset.getKeys().contains(key)))
            .toList();
        if (collectionKeys.isEmpty()) {
            return null;
        }

        List<String> unresolvedKeys = placeholders.stream()
            .filter(key -> !collectionKeys.contains(key))
            .filter(key -> !context.getScalarValues().containsKey(key))
            .toList();
        if (!unresolvedKeys.isEmpty()) {
            throw new BusinessCrmException(
                TemplateConstants.ErrorCodes.TEMPLATE_COLLECTION_PLACEHOLDERS_MISSING_DATASET,
                TemplateConstants.ErrorCodes.TEMPLATE_COLLECTION_PLACEHOLDERS_MISSING_DATASET,
                unresolvedKeys.toString()
            );
        }

        List<CollectionDataset> matchingDatasets = context.getCollections().stream()
            .filter(dataset -> dataset.getKeys().containsAll(collectionKeys))
            .toList();
        if (matchingDatasets.isEmpty()) {
            throw new BusinessCrmException(
                TemplateConstants.ErrorCodes.TEMPLATE_COLLECTION_PLACEHOLDERS_MISSING_DATASET,
                TemplateConstants.ErrorCodes.TEMPLATE_COLLECTION_PLACEHOLDERS_MISSING_DATASET,
                collectionKeys.toString()
            );
        }
        if (matchingDatasets.size() > 1) {
            throw new BusinessCrmException(
                TemplateConstants.ErrorCodes.TEMPLATE_COLLECTION_PLACEHOLDERS_AMBIGUOUS,
                TemplateConstants.ErrorCodes.TEMPLATE_COLLECTION_PLACEHOLDERS_AMBIGUOUS,
                collectionKeys.toString()
            );
        }
        return matchingDatasets.getFirst();
    }

    static Map<String, String> buildRowValues(
        Set<String> placeholders,
        GenerationTemplateContext context,
        CollectionDataset dataset,
        int itemIndex
    ) {
        Map<String, String> rowValues = new HashMap<>(context.getScalarValues());
        if (dataset == null || itemIndex >= dataset.getRows().size()) {
            return rowValues;
        }

        Map<String, String> datasetRow = dataset.getRows().get(itemIndex);
        for (String placeholder : placeholders) {
            if (dataset.getKeys().contains(placeholder)) {
                rowValues.put(placeholder, datasetRow.getOrDefault(placeholder, ""));
            }
        }
        return rowValues;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
