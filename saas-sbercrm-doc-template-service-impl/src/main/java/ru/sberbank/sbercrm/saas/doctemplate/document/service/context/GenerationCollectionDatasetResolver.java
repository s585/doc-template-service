package ru.sberbank.sbercrm.saas.doctemplate.document.service.context;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.sberbank.sbercrm.saas.doctemplate.application.exception.model.BusinessCrmException;
import ru.sberbank.sbercrm.saas.doctemplate.document.constant.DocumentConstants;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.CollectionDataset;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.CollectionQueryKey;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.TemplateMapping;

@Component
@RequiredArgsConstructor
public class GenerationCollectionDatasetResolver implements CollectionMappingClassifier {
    private final List<CollectionDatasetResolver> resolvers;

    @Override
    public boolean supports(TemplateMapping mapping) {
        return resolvers.stream().anyMatch(resolver -> resolver.supports(mapping));
    }

    @Override
    public CollectionQueryKey getQueryKey(TemplateMapping mapping) {
        return getResolver(mapping).getQueryKey(mapping);
    }

    public CollectionDataset resolve(
        List<TemplateMapping> mappings,
        Map<String, Object> sourceObject,
        UUID tenantId,
        UUID userId
    ) {
        if (mappings.isEmpty()) {
            return CollectionDataset.builder().build();
        }
        return getResolver(mappings.getFirst()).resolve(mappings, sourceObject, tenantId, userId);
    }

    private CollectionDatasetResolver getResolver(TemplateMapping mapping) {
        return resolvers.stream()
            .filter(resolver -> resolver.supports(mapping))
            .findFirst()
            .orElseThrow(() -> new BusinessCrmException(
                DocumentConstants.ErrorCodes.GENERATION_MAPPING_SOURCE_UNSUPPORTED,
                DocumentConstants.ErrorCodes.GENERATION_MAPPING_SOURCE_UNSUPPORTED,
                mapping.getKey()
            ));
    }
}
