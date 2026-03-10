package ru.sberbank.sbercrm.doctemplate.template;

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

import java.io.Serializable;
import java.util.UUID;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder({"id", "key", "scope", "value"})
@Schema(description = "Маппинг переменной шаблона: определяет область применения и способ получения значения")
public class TemplateMappingDto implements Serializable {
    private static final long serialVersionUID = 3888013418710878195L;

    @Schema(description = "Идентификатор маппинга")
    private UUID id;

    @NotBlank
    @Schema(description = "Ключ переменной в шаблоне")
    private String key;

    @NotNull
    @Schema(description = "Область применения переменной")
    private String scope;

    @Valid
    @NotNull
    @Schema(description = "Описание значения переменной")
    private MappingValueDto value;
}
