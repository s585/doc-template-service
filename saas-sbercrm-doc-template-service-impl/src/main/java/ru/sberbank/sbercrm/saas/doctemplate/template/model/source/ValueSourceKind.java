package ru.sberbank.sbercrm.saas.doctemplate.template.model.source;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.HashMap;
import java.util.Map;

public enum ValueSourceKind {
    DIRECT,
    REFERENCE,
    CONSTANT;

    private static final Map<String, ValueSourceKind> CONSTANTS = new HashMap<>();

    static {
        for (ValueSourceKind valueSourceKind : values()) {
            CONSTANTS.put(valueSourceKind.name(), valueSourceKind);
        }
    }

    @JsonCreator
    public static ValueSourceKind fromValue(String value) {
        ValueSourceKind valueSourceKind = CONSTANTS.get(value);
        if (valueSourceKind == null) {
            throw new IllegalArgumentException("Unsupported value source kind: " + value);
        }
        return valueSourceKind;
    }

    @JsonValue
    public String value() {
        return name();
    }
}
