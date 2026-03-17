package ru.sberbank.sbercrm.doctemplate.rule;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

import static ru.sberbank.sbercrm.doctemplate.rule.RuleDto.RuleType.PRIMITIVE;

@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@JsonPropertyOrder({"type", "value"})
@JsonTypeName(PRIMITIVE)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class PrimitiveRuleDto implements RuleDto, Serializable {
    private static final long serialVersionUID = 2378378942906566566L;
    private String value;

    @Override
    public String getType() {
        return PRIMITIVE;
    }
}
