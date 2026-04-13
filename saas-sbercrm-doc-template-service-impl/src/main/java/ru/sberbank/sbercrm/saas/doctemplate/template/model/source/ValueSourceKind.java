package ru.sberbank.sbercrm.saas.doctemplate.template.model.source;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;

import static java.util.stream.Collectors.toUnmodifiableMap;

public enum ValueSourceKind {
    DIRECT,
    REFERENCE,
    CONSTANT;

    private static final Map<String, ValueSourceKind> CONSTANTS = Arrays.stream(values())
        .collect(toUnmodifiableMap(ValueSourceKind::name, Function.identity()));

    @JsonCreator
    public static ValueSourceKind fromValue(String value) {
        ValueSourceKind kind = CONSTANTS.get(value);
        if (kind == null) {
            throw new IllegalArgumentException("Unsupported value source kind: " + value);
        }
        return kind;
    }

    @JsonValue
    public String value() {
        return name();
    }
}
