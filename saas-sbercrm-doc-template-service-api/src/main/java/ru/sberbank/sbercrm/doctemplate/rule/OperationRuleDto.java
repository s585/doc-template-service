package ru.sberbank.sbercrm.doctemplate.rule;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.Serializable;
import java.util.List;

import static ru.sberbank.sbercrm.doctemplate.rule.RuleDto.RuleType.OPERATION;

@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@JsonPropertyOrder({"type", "path", "op", "args"})
@JsonTypeName(OPERATION)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class OperationRuleDto implements RuleDto, HasArgs, Serializable {
    public static final OperationRuleDto TRUE = OperationRuleDto.builder().op(OperationTypeEnum.TRUE.value()).build();
    public static final OperationRuleDto FALSE = OperationRuleDto.builder().op(OperationTypeEnum.FALSE.value()).build();
    private static final long serialVersionUID = -6875912614551423455L;
    @Nullable
    @Schema(title = "Путь к полю, по которому будет проверяться условие", example = "source.sourceDeal")
    private String path;
    @Schema(title = "Операция сравнения", example = "equal")
    private String op;
    @Schema(title = "Значение, с которым будет сравниваться поле")
    private List<RuleDto> args;

    @Override
    public String getType() {
        return OPERATION;
    }
}
