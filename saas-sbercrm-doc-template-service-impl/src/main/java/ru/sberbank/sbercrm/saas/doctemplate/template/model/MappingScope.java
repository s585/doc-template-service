package ru.sberbank.sbercrm.saas.doctemplate.template.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;

import static java.util.stream.Collectors.toUnmodifiableMap;

public enum MappingScope {
    FILE_NAME("FILE_NAME"),
    VALUE("VALUE"),
    TABLE("TABLE");

    private static final Map<String, MappingScope> CONSTANTS = Arrays.stream(values())
        .collect(toUnmodifiableMap(MappingScope::value, Function.identity()));

    private final String value;

    MappingScope(String value) {
        this.value = value;
    }

    @JsonCreator
    public static MappingScope fromValue(String value) {
        MappingScope scope = CONSTANTS.get(value);
        if (scope == null) {
            throw new IllegalArgumentException("Unsupported mapping scope: " + value);
        }
        return scope;
    }

    @JsonValue
    public String value() {
        return value;
    }
}
