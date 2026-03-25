package ru.sberbank.sbercrm.doctemplate.template.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.checkerframework.checker.nullness.qual.Nullable;
import ru.sberbank.sbercrm.doctemplate.template.dto.expression.ExpressionDto;
import ru.sberbank.sbercrm.doctemplate.template.dto.source.ValueSourceDto;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder({"scope", "type", "expression", "source"})
@Schema(description = "Определение маппинга переменной шаблона")
public class TemplateMappingDefinitionDto {

    @Schema(description = "Область применения переменной")
    private String scope;

    @Schema(description = "Тип итогового значения")
    @Nullable
    private String type;

    @Valid
    @Schema(description = "Выражение для преобразования результата source. Выполняется после source")
    @Nullable
    private ExpressionDto expression;

    @Valid
    @Schema(description = "Источник данных для значения")
    @Nullable
    private ValueSourceDto source;
}
