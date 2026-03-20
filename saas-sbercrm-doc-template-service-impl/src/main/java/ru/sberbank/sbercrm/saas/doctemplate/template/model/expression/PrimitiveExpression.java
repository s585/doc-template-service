package ru.sberbank.sbercrm.saas.doctemplate.template.model.expression;

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
@JsonTypeName(TemplateConstants.ExpressionJsonTypes.PRIMITIVE)
@JsonPropertyOrder({"type", "value"})
public final class PrimitiveExpression implements Expression {
    private Object value;

    @Override
    public String getType() {
        return TemplateConstants.ExpressionJsonTypes.PRIMITIVE;
    }
}
