package ru.sberbank.sbercrm.saas.doctemplate.template.dto.expression;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.sberbank.sbercrm.saas.doctemplate.template.constant.TemplateApiConstants;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@JsonTypeName(TemplateApiConstants.ExpressionJsonTypes.OPERATION)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder({"type", "op", "args"})
@Schema(description = "Узел expression с оператором и аргументами")
public class OperationExpressionDto implements ExpressionDto {

    @Schema(description = "Оператор expression")
    @NotBlank
    private String op;

    @Valid
    @Builder.Default
    @Schema(description = "Аргументы оператора expression")
    private List<ExpressionDto> args = new ArrayList<>();

    @Override
    public String getType() {
        return TemplateApiConstants.ExpressionJsonTypes.OPERATION;
    }
}
