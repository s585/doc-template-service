package ru.sberbank.sbercrm.doctemplate.template.model.source;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.sberbank.sbercrm.doctemplate.common.PagingRqDto;
import ru.sberbank.sbercrm.doctemplate.common.SortTypeDto;

import java.util.List;
import java.util.UUID;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@JsonTypeName(ValueSourceJsonKinds.REFERENCE)
@JsonPropertyOrder({"kind", "targetPath", "entityId", "referenceFieldName", "referenceValuePath", "path", "sort", "paging"})
public final class ReferenceValueSource implements ValueSource {
    private String targetPath;
    private UUID entityId;
    private String referenceFieldName;
    private String referenceValuePath;
    private String path;
    private List<SortTypeDto> sort;
    private PagingRqDto paging;

    @Override
    public ValueSourceKind getKind() {
        return ValueSourceKind.REFERENCE;
    }
}
