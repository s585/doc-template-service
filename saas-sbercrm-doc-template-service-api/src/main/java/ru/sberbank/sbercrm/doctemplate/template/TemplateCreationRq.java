package ru.sberbank.sbercrm.doctemplate.template;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;
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
@JsonPropertyOrder({"entityId", "name", "description", "code"})
@Schema(description = "Метаданные запроса на импорт шаблона")
public class TemplateCreationRq implements Serializable {
    private static final long serialVersionUID = 4094228388911938422L;

    @NotNull
    @Schema(description = "Идентификатор сущности, к которой относится шаблон")
    private UUID entityId;

    @NotBlank
    @Schema(description = "Название шаблона")
    private String name;

    @Schema(description = "Описание шаблона")
    private String description;

    @NotBlank
    @Schema(description = "Системный код шаблона")
    private String code;
}
