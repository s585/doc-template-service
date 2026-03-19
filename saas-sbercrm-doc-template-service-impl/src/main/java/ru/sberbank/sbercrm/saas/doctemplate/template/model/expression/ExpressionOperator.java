package ru.sberbank.sbercrm.saas.doctemplate.template.model.expression;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.HashMap;
import java.util.Map;

public enum ExpressionOperator {
    COALESCE("coalesce"),
    FORMAT_DATE("formatDate"),
    UPPER("upper"),
    LOWER("lower"),
    TRIM("trim");

    private static final Map<String, ExpressionOperator> CONSTANTS = new HashMap<>();

    static {
        for (ExpressionOperator operator : values()) {
            CONSTANTS.put(operator.value, operator);
        }
    }

    private final String value;

    ExpressionOperator(String value) {
        this.value = value;
    }

    @JsonCreator
    public static ExpressionOperator fromValue(String value) {
        ExpressionOperator operator = CONSTANTS.get(value);
        if (operator == null) {
            throw new IllegalArgumentException("Unsupported expression operator: " + value);
        }
        return operator;
    }

    @JsonValue
    public String value() {
        return value;
    }
}
