package ru.sberbank.sbercrm.doctemplate.rule;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import static ru.sberbank.sbercrm.doctemplate.rule.RuleDto.RuleType.*;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type")
@JsonSubTypes(
    {
        @JsonSubTypes.Type(value = OperationRuleDto.class, name = OPERATION),
        @JsonSubTypes.Type(value = PrimitiveRuleDto.class, name = PRIMITIVE),
        @JsonSubTypes.Type(value = ArrayRuleDto.class, name = ARRAY)
    }
    )
public interface RuleDto {
    @JsonProperty("type")
    String getType();

    final class RuleType {
        public static final String OPERATION = "OPERATION";
        public static final String PRIMITIVE = "PRIMITIVE";
        public static final String ARRAY = "ARRAY";
    }
}
