package ru.sberbank.sbercrm.saas.doctemplate.template.model.expression;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.sberbank.sbercrm.doctemplate.template.constant.TemplateApiConstants;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@JsonTypeName(TemplateApiConstants.ExpressionJsonTypes.PRIMITIVE)
@JsonPropertyOrder({"type", "value"})
public final class PrimitiveExpression implements Expression {
    private Object value;

    @Override
    public String getType() {
        return TemplateApiConstants.ExpressionJsonTypes.PRIMITIVE;
    }
}
