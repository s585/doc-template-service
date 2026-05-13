package ru.sberbank.sbercrm.saas.doctemplate.document.service.context;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.sberbank.sbercrm.saas.doctemplate.application.integration.gateway.BusinessObjectGateway;
import ru.sberbank.sbercrm.saas.doctemplate.application.pagination.PageIterator;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.CollectionDataset;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.CollectionQueryKey;
import ru.sberbank.sbercrm.saas.doctemplate.document.service.context.expression.ExpressionEvaluator;
import ru.sberbank.sbercrm.saas.doctemplate.shared.dto.CommonRqDto;
import ru.sberbank.sbercrm.saas.doctemplate.shared.dto.FilterDto;
import ru.sberbank.sbercrm.saas.doctemplate.shared.dto.PagingRqDto;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.TemplateMapping;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.source.ReferenceValueSource;

@Component
@RequiredArgsConstructor
public class ReferenceCollectionDatasetResolver implements CollectionDatasetResolver {
    private final BusinessObjectGateway businessObjectGateway;
    private final GenerationPathResolver generationPathResolver;
    private final ExpressionEvaluator expressionEvaluator;

    @Override
    public boolean supports(TemplateMapping mapping) {
        return mapping.getDefinition() != null && mapping.getDefinition().getSource() instanceof ReferenceValueSource;
    }

    @Override
    public CollectionQueryKey getQueryKey(TemplateMapping mapping) {
        return CollectionQueryKey.from((ReferenceValueSource) mapping.getDefinition().getSource());
    }

    @Override
    public CollectionDataset resolve(
        List<TemplateMapping> mappings,
        Map<String, Object> sourceObject,
        UUID tenantId,
        UUID userId
    ) {
        CollectionDataset dataset = initializeDataset(mappings);
        if (mappings.isEmpty() || sourceObject == null) {
            return dataset;
        }

        ReferenceValueSource referenceSource = (ReferenceValueSource) mappings.getFirst().getDefinition().getSource();
        Object referenceValue = generationPathResolver.resolveSourcePath(
            sourceObject,
            referenceSource.getReferenceValuePath(),
            mappings.getFirst().getKey()
        );
        if (referenceValue == null || isBlankString(referenceValue)) {
            return dataset;
        }

        CommonRqDto request = buildRequest(referenceSource, referenceValue);
        for (List<Map<String, Object>> page : PageIterator.iteratePages(
            request,
            pageRequest -> businessObjectGateway.getListObjectsPage(
                tenantId,
                userId,
                referenceSource.getEntityId(),
                pageRequest
            )
        )) {
            for (Map<String, Object> referenceObject : page) {
                Map<String, String> row = new LinkedHashMap<>();
                for (TemplateMapping mapping : mappings) {
                    ReferenceValueSource mappingSource = (ReferenceValueSource) mapping.getDefinition().getSource();
                    Object resolvedValue = generationPathResolver.resolveReferencePath(
                        referenceObject,
                        mappingSource.getPath(),
                        mapping.getKey()
                    );
                    Object evaluatedValue = expressionEvaluator.evaluate(mapping, resolvedValue);
                    row.put(mapping.getKey(), toStringValue(evaluatedValue));
                }
                dataset.getRows().add(row);
            }
        }
        return dataset;
    }

    private CollectionDataset initializeDataset(List<TemplateMapping> mappings) {
        if (mappings.isEmpty()) {
            return CollectionDataset.builder().build();
        }
        return CollectionDataset.builder()
            .queryKey(getQueryKey(mappings.getFirst()))
            .keys(mappings.stream()
                .map(TemplateMapping::getKey)
                .collect(Collectors.toCollection(LinkedHashSet::new)))
            .rows(new ArrayList<>())
            .build();
    }

    private CommonRqDto buildRequest(ReferenceValueSource referenceSource, Object referenceValue) {
        PagingRqDto paging = referenceSource.getPaging() == null
            ? PagingRqDto.builder().page(0).size(100).build()
            : referenceSource.getPaging().toBuilder().page(0).build();
        return CommonRqDto.builder()
            .filter(Set.of(
                FilterDto.builder()
                    .field(referenceSource.getReferenceFieldName())
                    .operation(FilterDto.Operation.EQUAL)
                    .value(List.of(referenceValue))
                    .build()
            ))
            .sort(referenceSource.getSort() == null ? List.of() : referenceSource.getSort())
            .paging(paging)
            .build();
    }

    private boolean isBlankString(Object value) {
        return value instanceof String stringValue && stringValue.isBlank();
    }

    private String toStringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
