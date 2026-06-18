package ru.sberbank.sbercrm.saas.doctemplate.template.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.checkerframework.checker.nullness.qual.Nullable;
import ru.sberbank.sbercrm.saas.doctemplate.template.dto.expression.ExpressionDto;
import ru.sberbank.sbercrm.saas.doctemplate.template.dto.source.ValueSourceDto;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder({"scope", "type", "expression", "source", "layout"})
@Schema(description = "Определение маппинга переменной шаблона")
public class TemplateMappingDefinitionDto {

    @NotBlank
    @Schema(description = "Область применения переменной")
    private String scope;

    @NotBlank
    @Schema(description = "Тип итогового значения")
    private String type;

    @Valid
    @Schema(description = "Выражение для преобразования результата source. Выполняется после source")
    @Nullable
    private ExpressionDto expression;

    @Valid
    @Schema(description = "Источник данных для значения")
    @Nullable
    private ValueSourceDto source;

    @Valid
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Schema(description = "Read-only layout metadata calculated from the imported template file", accessMode = Schema.AccessMode.READ_ONLY)
    @Nullable
    private TemplateMappingLayoutDto layout;
}
