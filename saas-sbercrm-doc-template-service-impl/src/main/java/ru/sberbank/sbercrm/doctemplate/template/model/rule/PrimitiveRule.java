package ru.sberbank.sbercrm.doctemplate.template.model.rule;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.sberbank.sbercrm.doctemplate.template.constant.rule.RuleJsonTypes;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@JsonTypeName(RuleJsonTypes.PRIMITIVE)
@JsonPropertyOrder({"type", "value"})
public final class PrimitiveRule implements Rule {
    private String value;

    @Override
    public RuleType getType() {
        return RuleType.PRIMITIVE;
    }
}
