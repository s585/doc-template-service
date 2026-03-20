package ru.sberbank.sbercrm.doctemplate.template.dto.expression;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.Schema;

import static ru.sberbank.sbercrm.doctemplate.template.dto.expression.ExpressionDto.ExpressionNodeType.OPERATION;
import static ru.sberbank.sbercrm.doctemplate.template.dto.expression.ExpressionDto.ExpressionNodeType.PRIMITIVE;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = OperationExpressionDto.class, name = OPERATION),
    @JsonSubTypes.Type(value = PrimitiveExpressionDto.class, name = PRIMITIVE)
})
public interface ExpressionDto {
    @Schema(description = "Тип узла выражения")
    @JsonProperty("type")
    String getType();

    final class ExpressionNodeType {
        public static final String OPERATION = "operation";
        public static final String PRIMITIVE = "primitive";

        private ExpressionNodeType() {
        }
    }
}
