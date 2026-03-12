package ru.sberbank.sbercrm.doctemplate.template.model.source;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ValueSourceKind {
    DIRECT,
    REFERENCE,
    CONSTANT;

    @JsonCreator
    public static ValueSourceKind fromValue(String value) {
        return ValueSourceKind.valueOf(value);
    }

    @JsonValue
    public String value() {
        return name();
    }
}
