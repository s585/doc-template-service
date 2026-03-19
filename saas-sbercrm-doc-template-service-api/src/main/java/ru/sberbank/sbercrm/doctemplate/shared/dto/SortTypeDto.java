package ru.sberbank.sbercrm.doctemplate.shared.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder({"field", "direction"})
@Schema(description = "Параметр сортировки")
public class SortTypeDto implements Serializable {
    private static final long serialVersionUID = -8093169857923577362L;

    @NotBlank
    @Schema(description = "Имя поля сортировки")
    private String field;

    @NotNull
    @Schema(description = "Направление сортировки")
    private Direction direction;

    @Schema(description = "Направление сортировки")
    public enum Direction {
        ASC("ASC"),
        DESC("DESC");

        private static final Map<String, Direction> CONSTANTS = new HashMap<>();

        static {
            for (Direction direction : values()) {
                CONSTANTS.put(direction.value, direction);
            }
        }

        private final String value;

        Direction(String value) {
            this.value = value;
        }

        @JsonCreator
        public static Direction fromValue(String value) {
            Direction constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            }
            return constant;
        }

        @JsonValue
        public String value() {
            return value;
        }
    }
}
