package ru.sberbank.sbercrm.doctemplate.template.model.expression;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import ru.sberbank.sbercrm.doctemplate.template.constant.expression.ExpressionJsonTypes;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = OperationExpression.class, name = ExpressionJsonTypes.OPERATION),
    @JsonSubTypes.Type(value = PrimitiveExpression.class, name = ExpressionJsonTypes.PRIMITIVE)
})
public sealed interface Expression permits OperationExpression, PrimitiveExpression {
    @JsonProperty("type")
    String getType();
}
