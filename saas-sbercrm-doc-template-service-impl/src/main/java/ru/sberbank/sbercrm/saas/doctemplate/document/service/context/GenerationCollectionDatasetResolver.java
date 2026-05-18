package ru.sberbank.sbercrm.saas.doctemplate.document.service.context;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.sberbank.sbercrm.saas.doctemplate.application.exception.model.BusinessCrmException;
import ru.sberbank.sbercrm.saas.doctemplate.document.constant.DocumentConstants;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.CollectionDataset;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.CollectionQueryKey;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.TemplateMapping;

/**
 * Facade для обработчиков collection dataset-ов.
 *
 * <p>Выбирает конкретный обработчик по source mapping-а, используется одновременно как classifier
 * при построении плана generation mapping-ов и как executor при сборке collection dataset-ов.
 */
@Component
@Slf4j
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

    /**
     * Собирает dataset для группы mapping-ов, которые уже имеют общий {@link CollectionQueryKey}.
     */
    public CollectionDataset resolve(
        List<TemplateMapping> mappings,
        Map<String, Object> sourceObject,
        UUID tenantId,
        UUID userId
    ) {
        if (mappings.isEmpty()) {
            return CollectionDataset.builder().build();
        }
        TemplateMapping firstMapping = mappings.getFirst();
        CollectionDatasetResolver resolver = getResolver(firstMapping);
        log.debug(
            "Resolving generation collection dataset: queryKey={}, mappingCount={}, resolver={}",
            resolver.getQueryKey(firstMapping),
            mappings.size(),
            resolver.getClass().getSimpleName()
        );
        CollectionDataset dataset = resolver.resolve(mappings, sourceObject, tenantId, userId);
        log.debug(
            "Resolved generation collection dataset: keys={}, rowCount={}, resolver={}",
            dataset.getKeys(),
            dataset.getRows().size(),
            resolver.getClass().getSimpleName()
        );
        return dataset;
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
