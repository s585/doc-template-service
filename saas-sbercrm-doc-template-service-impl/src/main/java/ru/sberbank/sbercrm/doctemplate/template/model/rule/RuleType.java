package ru.sberbank.sbercrm.doctemplate.template.model.rule;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum RuleType {
    OPERATION,
    PRIMITIVE;

    @JsonCreator
    public static RuleType fromValue(String value) {
        return RuleType.valueOf(value);
    }

    @JsonValue
    public String value() {
        return name();
    }
}
