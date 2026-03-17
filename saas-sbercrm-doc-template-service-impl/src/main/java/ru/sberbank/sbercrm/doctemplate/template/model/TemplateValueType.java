package ru.sberbank.sbercrm.doctemplate.template.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.HashMap;
import java.util.Map;

public enum TemplateValueType {
    STRING("STRING"),
    NUMBER("NUMBER"),
    DATE("DATE"),
    DATETIME("DATETIME"),
    BOOLEAN("BOOLEAN");

    private static final Map<String, TemplateValueType> CONSTANTS = new HashMap<>();

    static {
        for (TemplateValueType type : values()) {
            CONSTANTS.put(type.value, type);
        }
    }

    private final String value;

    TemplateValueType(String value) {
        this.value = value;
    }

    @JsonCreator
    public static TemplateValueType fromValue(String value) {
        TemplateValueType type = CONSTANTS.get(value);
        if (type == null) {
            throw new IllegalArgumentException("Unsupported template value type: " + value);
        }
        return type;
    }

    @JsonValue
    public String value() {
        return value;
    }
}
