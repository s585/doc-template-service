package ru.sberbank.sbercrm.saas.doctemplate.template.model.source;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.sberbank.sbercrm.saas.doctemplate.template.constant.TemplateConstants;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@JsonTypeName(TemplateConstants.ValueSourceJsonKinds.CONSTANT)
@JsonPropertyOrder({"kind", "value"})
public final class ConstantValueSource implements ValueSource {
    private Object value;

    @Override
    public ValueSourceKind getKind() {
        return ValueSourceKind.CONSTANT;
    }
}
