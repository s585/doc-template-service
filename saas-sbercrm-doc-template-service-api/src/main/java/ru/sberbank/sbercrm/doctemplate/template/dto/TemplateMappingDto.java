package ru.sberbank.sbercrm.doctemplate.template.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.UUID;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder({"id", "key", "definition"})
@Schema(description = "Маппинг переменной шаблона: определяет область применения и способ получения значения")
public class TemplateMappingDto {

    @Schema(description = "Идентификатор маппинга")
    @Nullable
    private UUID id;

    @NotBlank
    @Schema(description = "Ключ переменной в шаблоне")
    private String key;

    @Valid
    @NotNull
    @Schema(description = "Определение маппинга переменной")
    private TemplateMappingDefinitionDto definition;
}
