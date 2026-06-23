package ru.sberbank.sbercrm.saas.doctemplate.template.model.expression;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;

import static java.util.stream.Collectors.toUnmodifiableMap;

public enum ExpressionOperator {
    COALESCE("coalesce"),
    CONCAT("concat"),
    FORMAT_DATE("formatDate"),
    UPPER("upper"),
    LOWER("lower"),
    TRIM("trim");

    private static final Map<String, ExpressionOperator> CONSTANTS = Arrays.stream(values())
        .collect(toUnmodifiableMap(ExpressionOperator::value, Function.identity()));

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
