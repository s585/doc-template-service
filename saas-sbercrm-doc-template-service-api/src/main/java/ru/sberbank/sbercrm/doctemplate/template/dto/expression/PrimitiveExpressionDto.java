package ru.sberbank.sbercrm.doctemplate.template.dto.expression;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

import static ru.sberbank.sbercrm.doctemplate.template.dto.expression.ExpressionDto.ExpressionNodeType.PRIMITIVE;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@JsonTypeName(PRIMITIVE)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder({"type", "value"})
@Schema(description = "Примитивный узел expression")
public class PrimitiveExpressionDto implements ExpressionDto, Serializable {
    private static final long serialVersionUID = 2654605177012975096L;

    @Schema(description = "Значение примитива или специальный маркер $value", example = "$value")
    private Object value;

    @Override
    public String getType() {
        return PRIMITIVE;
    }
}
