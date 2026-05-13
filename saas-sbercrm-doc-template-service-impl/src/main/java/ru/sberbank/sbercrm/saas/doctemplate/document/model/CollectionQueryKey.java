package ru.sberbank.sbercrm.saas.doctemplate.document.model;

import java.util.List;
import java.util.UUID;
import ru.sberbank.sbercrm.saas.doctemplate.shared.dto.FilterDto;
import ru.sberbank.sbercrm.saas.doctemplate.shared.dto.PagingRqDto;
import ru.sberbank.sbercrm.saas.doctemplate.shared.dto.SortTypeDto;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.source.ReferenceValueSource;

public record CollectionQueryKey(
    UUID entityId,
    String targetPath,
    String referenceFieldName,
    String referenceValuePath,
    List<FilterDto> filters,
    List<SortTypeDto> sort,
    PagingRqDto paging
) {
    public static CollectionQueryKey from(ReferenceValueSource source) {
        return new CollectionQueryKey(
            source.getEntityId(),
            source.getTargetPath(),
            source.getReferenceFieldName(),
            source.getReferenceValuePath(),
            List.of(),
            source.getSort() == null ? List.of() : List.copyOf(source.getSort()),
            source.getPaging() == null ? PagingRqDto.builder().page(0).size(100).build() : source.getPaging().toBuilder().build()
        );
    }
}
