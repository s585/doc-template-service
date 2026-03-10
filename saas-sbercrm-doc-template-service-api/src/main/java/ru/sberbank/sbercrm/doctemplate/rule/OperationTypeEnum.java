package ru.sberbank.sbercrm.doctemplate.rule;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import ru.sberbank.sbercrm.doctemplate.common.FilterDto;

import java.util.HashMap;
import java.util.Map;

public enum OperationTypeEnum {
    GT("gt"),
    GTE("gte"),
    LT("lt"),
    LTE("lte"),
    EQUAL("equal"),
    EQUAL_DAY_OF_MONTH("equal day of month"),
    EQUAL_MONTH("equal month"),
    EQUAL_YEAR("equal year"),
    NOT_EQUAL("not equal"),
    CONTAINS("contains"),
    CONTAINS_ANY("contains_any"),
    IN("in"),
    NOT_IN("not in"),
    IS_NULL("is null"),
    IS_NOT_NULL("is not null"),
    IS_EMPTY("is empty"),
    IS_NOT_EMPTY("is not empty"),
    BETWEEN("between"),
    OVERLAPS("overlaps"),
    IS_NULL_OR_NOT_EQUAL("is null or not equal"),
    STARTS_WITH("starts with"),
    ENDS_WITH("ends with"), OR("or", true),
    AND("and", true),
    NOT("not", true),
    TRUE("true"),
    FALSE("false");

    private static final Map<String, OperationTypeEnum> CONSTANTS = new HashMap<>();

    static {
        for (OperationTypeEnum c : values()) {
            CONSTANTS.put(c.value, c);
        }
    }

    private final String value;
    @Getter
    @JsonIgnore
    private final boolean valueFilter;

    OperationTypeEnum(String value) {
        this.value = value;
        this.valueFilter = false;
    }

    OperationTypeEnum(String value, boolean isValueFilter) {
        this.value = value;
        this.valueFilter = isValueFilter;
    }

    @JsonCreator
    public static OperationTypeEnum fromValue(String value) {
        OperationTypeEnum constant = CONSTANTS.get(value);
        if (constant == null) {
            throw new IllegalArgumentException(value);
        } else {
            return constant;
        }
    }

    @Override
    public String toString() {
        return this.value;
    }

    @JsonValue
    public String value() {
        return this.value;
    }
}
