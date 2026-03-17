package ru.sberbank.sbercrm.doctemplate.template.model.source;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.sberbank.sbercrm.doctemplate.template.constant.source.ValueSourceJsonKinds;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@JsonTypeName(ValueSourceJsonKinds.DIRECT)
@JsonPropertyOrder({"kind", "path"})
public final class DirectValueSource implements ValueSource {
    private String path;

    @Override
    public ValueSourceKind getKind() {
        return ValueSourceKind.DIRECT;
    }
}
