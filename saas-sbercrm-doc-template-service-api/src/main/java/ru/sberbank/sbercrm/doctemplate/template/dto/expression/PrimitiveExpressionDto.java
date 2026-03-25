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
import ru.sberbank.sbercrm.doctemplate.template.constant.TemplateApiConstants;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@JsonTypeName(TemplateApiConstants.ExpressionJsonTypes.PRIMITIVE)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder({"type", "value"})
@Schema(description = "Примитивный узел expression")
public class PrimitiveExpressionDto implements ExpressionDto {

    @Schema(description = "Значение примитива или специальный маркер $value", example = "$value")
    private Object value;

    @Override
    public String getType() {
        return TemplateApiConstants.ExpressionJsonTypes.PRIMITIVE;
    }
}
