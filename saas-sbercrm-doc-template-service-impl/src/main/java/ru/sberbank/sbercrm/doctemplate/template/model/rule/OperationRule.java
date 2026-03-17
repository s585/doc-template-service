package ru.sberbank.sbercrm.doctemplate.template.model.rule;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.sberbank.sbercrm.doctemplate.template.constant.rule.RuleJsonTypes;

import java.util.List;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@JsonTypeName(RuleJsonTypes.OPERATION)
@JsonPropertyOrder({"type", "path", "op", "args"})
public final class OperationRule implements Rule {
    private String path;
    private String op;
    private List<Rule> args;

    @Override
    public RuleType getType() {
        return RuleType.OPERATION;
    }
}
