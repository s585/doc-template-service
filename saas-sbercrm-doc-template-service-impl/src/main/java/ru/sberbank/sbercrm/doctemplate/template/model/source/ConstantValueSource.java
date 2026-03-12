package ru.sberbank.sbercrm.doctemplate.template.model.source;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@JsonTypeName(ValueSourceJsonKinds.CONSTANT)
@JsonPropertyOrder({"kind", "value"})
public final class ConstantValueSource implements ValueSource {
    private Object value;

    @Override
    public ValueSourceKind getKind() {
        return ValueSourceKind.CONSTANT;
    }
}
