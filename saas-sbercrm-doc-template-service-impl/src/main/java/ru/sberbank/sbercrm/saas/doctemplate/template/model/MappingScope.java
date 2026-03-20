package ru.sberbank.sbercrm.saas.doctemplate.template.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.HashMap;
import java.util.Map;

public enum MappingScope {
    FILE_NAME("FILE_NAME"),
    VALUE("VALUE"),
    TABLE("TABLE");

    private static final Map<String, MappingScope> CONSTANTS = new HashMap<>();

    static {
        for (MappingScope scope : values()) {
            CONSTANTS.put(scope.value, scope);
        }
    }

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
