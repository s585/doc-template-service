package ru.sberbank.sbercrm.doctemplate.template;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.checkerframework.checker.nullness.qual.Nullable;
import ru.sberbank.sbercrm.doctemplate.expression.ExpressionDto;
import ru.sberbank.sbercrm.doctemplate.template.source.ValueSourceDto;

import java.io.Serializable;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder({"scope", "type", "expression", "source"})
@Schema(description = "Определение маппинга переменной шаблона")
public class TemplateMappingDefinitionDto implements Serializable {
    private static final long serialVersionUID = 1231723713516335699L;

    @NotNull
    @Schema(description = "Область применения переменной")
    private String scope;

    @NotNull
    @Schema(description = "Тип итогового значения")
    private String type;

    @Valid
    @Schema(description = "Выражение для преобразования результата source. Выполняется после source")
    @Nullable
    private ExpressionDto expression;

    @Valid
    @NotNull
    @Schema(description = "Источник данных для значения")
    private ValueSourceDto source;
}
