package ru.sberbank.sbercrm.doctemplate.template.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.HashMap;
import java.util.Map;

public enum TemplateFormat {
    DOCX("DOCX"),
    XLSX("XLSX");

    private static final Map<String, TemplateFormat> CONSTANTS = new HashMap<>();

    static {
        for (TemplateFormat format : values()) {
            CONSTANTS.put(format.value, format);
        }
    }

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
