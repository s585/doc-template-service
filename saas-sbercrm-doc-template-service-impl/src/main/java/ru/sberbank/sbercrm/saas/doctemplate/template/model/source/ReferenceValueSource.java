package ru.sberbank.sbercrm.saas.doctemplate.template.model.source;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.sberbank.sbercrm.saas.doctemplate.template.constant.TemplateApiConstants;
import ru.sberbank.sbercrm.saas.doctemplate.shared.dto.PagingRqDto;
import ru.sberbank.sbercrm.saas.doctemplate.shared.dto.SortTypeDto;

import java.util.List;
import java.util.UUID;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@JsonTypeName(TemplateApiConstants.ValueSourceJsonKinds.REFERENCE)
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
