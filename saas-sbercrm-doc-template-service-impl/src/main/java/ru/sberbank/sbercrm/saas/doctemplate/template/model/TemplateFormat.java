package ru.sberbank.sbercrm.saas.doctemplate.template.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;

import static java.util.stream.Collectors.toUnmodifiableMap;

public enum TemplateFormat {
    DOCX("DOCX"),
    XLSX("XLSX");

    private static final Map<String, TemplateFormat> CONSTANTS = Arrays.stream(values())
        .collect(toUnmodifiableMap(TemplateFormat::value, Function.identity()));

    private final String value;

    TemplateFormat(String value) {
        this.value = value;
    }

    @JsonCreator
    public static TemplateFormat fromValue(String value) {
        TemplateFormat format = CONSTANTS.get(value);
        if (format == null) {
            throw new IllegalArgumentException("Unsupported template format: " + value);
        }
        return format;
    }

    @JsonValue
    public String value() {
        return value;
    }
}
