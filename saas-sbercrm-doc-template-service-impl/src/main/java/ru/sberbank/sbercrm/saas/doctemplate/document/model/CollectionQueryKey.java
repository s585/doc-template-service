package ru.sberbank.sbercrm.saas.doctemplate.document.model;

import java.util.List;
import java.util.UUID;
import ru.sberbank.sbercrm.saas.doctemplate.shared.dto.FilterDto;
import ru.sberbank.sbercrm.saas.doctemplate.shared.dto.PagingRqDto;
import ru.sberbank.sbercrm.saas.doctemplate.shared.dto.SortTypeDto;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.source.ReferenceValueSource;

/**
 * Ключ группировки collection mapping-ов, которые можно загрузить одним запросом к business object API.
 *
 * <p>Если несколько placeholder-ов используют один и тот же reference source, они попадают в один
 * dataset и не создают дублирующие постраничные запросы.
 */
public record CollectionQueryKey(
    UUID entityId,
    String targetPath,
    String referenceFieldName,
    String referenceValuePath,
    List<FilterDto> filters,
    List<SortTypeDto> sort,
    PagingRqDto paging
) {
    /**
     * Строит ключ группировки из reference source mapping-а.
     */
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
