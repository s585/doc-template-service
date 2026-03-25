package ru.sberbank.sbercrm.doctemplate.template.dto.expression;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import ru.sberbank.sbercrm.doctemplate.template.constant.TemplateApiConstants;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = OperationExpressionDto.class, name = TemplateApiConstants.ExpressionJsonTypes.OPERATION),
    @JsonSubTypes.Type(value = PrimitiveExpressionDto.class, name = TemplateApiConstants.ExpressionJsonTypes.PRIMITIVE)
})
public interface ExpressionDto {
    @Schema(description = "Тип узла выражения")
    @JsonProperty("type")
    String getType();
}
