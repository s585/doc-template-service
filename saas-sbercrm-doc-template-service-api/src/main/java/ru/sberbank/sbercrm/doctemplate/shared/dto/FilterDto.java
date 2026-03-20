package ru.sberbank.sbercrm.doctemplate.shared.dto;


import com.fasterxml.jackson.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import org.checkerframework.checker.nullness.qual.Nullable;

import ru.sberbank.sbercrm.doctemplate.shared.contract.HasField;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Фильтр записей.
 */
@With
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"field", "secondField", "value", "operation", "valueConverter"})
@JsonIgnoreProperties(ignoreUnknown = true)
public class FilterDto implements Serializable, HasField {
    private static final long serialVersionUID = 1160752734340956275L;
    private static final String FIELD_NAME_REGEXP = "^[a-zA-Z0-9.$:^_]+";
    @Nullable
    @Schema(title = "Название поля", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @Pattern(regexp = FIELD_NAME_REGEXP)
    private String field;
    @Schema(title = "Название поля для операций с двумя полями (OVERLAPS)")
    @Pattern(regexp = FIELD_NAME_REGEXP)
    private String secondField;
    @Schema(title = "Тип операции")
    private Operation operation;
    @Schema(title = "Значение поля")
    @Builder.Default
    private List<Object> value = new ArrayList<>();
    @Nullable
    @Schema(title = "Конвертация поля", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String valueConverter;

    public FilterDto(@Nullable String field, @Nullable Operation operation, @Nullable List<Object> value) {
        this.field = field;
        this.operation = operation;
        this.value = value == null ? new ArrayList<>() : value;
    }

    @JsonIgnore
    public boolean isValueFilter() {
        return operation != null && operation.isValueFilter();
    }

    public enum Operation {
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

        private static final Map<String, Operation> CONSTANTS = new HashMap<>();

        static {
            for (Operation c : values()) {
                CONSTANTS.put(c.value, c);
            }
        }

        private final String value;
        @Getter
        @JsonIgnore
        private final boolean valueFilter;

        Operation(String value) {
            this.value = value;
            this.valueFilter = false;
        }

        Operation(String value, boolean isValueFilter) {
            this.value = value;
            this.valueFilter = isValueFilter;
        }

        @JsonCreator
        public static Operation fromValue(String value) {
            Operation constant = CONSTANTS.get(value);
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
}
