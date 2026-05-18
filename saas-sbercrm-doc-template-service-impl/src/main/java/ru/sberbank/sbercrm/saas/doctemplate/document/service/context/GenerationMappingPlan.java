package ru.sberbank.sbercrm.saas.doctemplate.document.service.context;

import java.util.List;
import java.util.Map;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.CollectionQueryKey;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.TemplateMapping;

/**
 * Результат предварительной классификации mapping-ов шаблона перед сборкой generation context.
 *
 * <p>Разделяет обычные scalar mapping-и, mapping-и имени файла и группы collection mapping-ов,
 * которые должны загружаться общим запросом к источнику данных.
 */
public record GenerationMappingPlan(
    List<TemplateMapping> scalarMappings,
    List<TemplateMapping> generatedFileNameMappings,
    Map<CollectionQueryKey, List<TemplateMapping>> collectionGroups
) {
}
