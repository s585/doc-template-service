package ru.sberbank.sbercrm.saas.doctemplate.document.service.context;

import java.util.List;
import java.util.Map;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.CollectionQueryKey;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.TemplateMapping;

public record GenerationMappingPlan(
    List<TemplateMapping> scalarMappings,
    List<TemplateMapping> generatedFileNameMappings,
    Map<CollectionQueryKey, List<TemplateMapping>> collectionGroups
) {
}
