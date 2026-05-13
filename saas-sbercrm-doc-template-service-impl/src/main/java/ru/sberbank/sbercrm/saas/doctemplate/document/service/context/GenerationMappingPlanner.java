package ru.sberbank.sbercrm.saas.doctemplate.document.service.context;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.CollectionQueryKey;
import ru.sberbank.sbercrm.saas.doctemplate.template.constant.TemplateConstants;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.MappingScope;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.TemplateMapping;

@Component
@RequiredArgsConstructor
public class GenerationMappingPlanner {
    private final CollectionMappingClassifier collectionMappingClassifier;

    public GenerationMappingPlan build(List<TemplateMapping> mappings) {
        List<TemplateMapping> scalarMappings = new ArrayList<>();
        List<TemplateMapping> generatedFileNameMappings = new ArrayList<>();
        Map<CollectionQueryKey, List<TemplateMapping>> collectionGroups = new LinkedHashMap<>();

        for (TemplateMapping mapping : mappings) {
            if (isGeneratedFileNameMapping(mapping)) {
                generatedFileNameMappings.add(mapping);
                continue;
            }
            if (isResolvableCollectionMapping(mapping)) {
                CollectionQueryKey queryKey = collectionMappingClassifier.getQueryKey(mapping);
                collectionGroups.computeIfAbsent(queryKey, ignored -> new ArrayList<>()).add(mapping);
                continue;
            }
            scalarMappings.add(mapping);
        }

        return new GenerationMappingPlan(scalarMappings, generatedFileNameMappings, collectionGroups);
    }

    private boolean isResolvableCollectionMapping(TemplateMapping mapping) {
        return mapping.getDefinition() != null
            && mapping.getDefinition().getScope() == MappingScope.COLLECTION
            && mapping.getDefinition().getSource() != null
            && collectionMappingClassifier.supports(mapping);
    }

    private boolean isGeneratedFileNameMapping(TemplateMapping mapping) {
        return TemplateConstants.MappingKeys.GENERATED_FILE_NAME.equals(mapping.getKey());
    }
}
