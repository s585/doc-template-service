package ru.sberbank.sbercrm.saas.doctemplate.template.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;

import static java.util.stream.Collectors.toUnmodifiableMap;

public enum TemplateValueType {
    STRING("STRING"),
    NUMBER("NUMBER"),
    DATE("DATE"),
    DATETIME("DATETIME"),
    BOOLEAN("BOOLEAN");

    private static final Map<String, TemplateValueType> CONSTANTS = Arrays.stream(values())
        .collect(toUnmodifiableMap(TemplateValueType::value, Function.identity()));

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
