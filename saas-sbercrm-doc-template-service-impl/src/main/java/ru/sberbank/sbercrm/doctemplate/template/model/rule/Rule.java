package ru.sberbank.sbercrm.doctemplate.template.model.rule;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = OperationRule.class, name = RuleJsonTypes.OPERATION),
    @JsonSubTypes.Type(value = PrimitiveRule.class, name = RuleJsonTypes.PRIMITIVE)
})
public sealed interface Rule permits OperationRule, PrimitiveRule {
    @JsonProperty("type")
    RuleType getType();
}
