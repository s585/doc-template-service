package ru.sberbank.sbercrm.saas.doctemplate.document.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder({"templateId", "entityId", "objectId", "requestId", "formats"})
@Schema(title = "Запрос на создание документа")
public class DocumentCreationRq {

    @NotNull
    @Schema(title = "Идентификатор шаблона")
    private UUID templateId;

    @NotNull
    @Schema(title = "Идентификатор сущности исходного объекта")
    private UUID entityId;

    @NotNull
    @Schema(title = "Идентификатор исходного объекта")
    private UUID objectId;

    @NotNull
    @Schema(title = "Идентификатор идемпотентного запроса")
    private UUID requestId;

    @NotEmpty
    @Builder.Default
    @ArraySchema(schema = @Schema(title = "Список форматов документа"))
    private List<String> formats = new ArrayList<>();
}
