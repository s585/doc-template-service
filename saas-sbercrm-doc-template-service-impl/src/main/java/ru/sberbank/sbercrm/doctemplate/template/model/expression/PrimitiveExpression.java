package ru.sberbank.sbercrm.doctemplate.template.model.expression;

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
@JsonTypeName(ExpressionJsonTypes.PRIMITIVE)
@JsonPropertyOrder({"type", "value"})
public final class PrimitiveExpression implements Expression {
    private Object value;

    @Override
    public String getType() {
        return ExpressionJsonTypes.PRIMITIVE;
    }
}
