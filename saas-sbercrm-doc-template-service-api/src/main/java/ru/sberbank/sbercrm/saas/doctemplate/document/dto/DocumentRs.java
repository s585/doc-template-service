package ru.sberbank.sbercrm.saas.doctemplate.document.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder({
    "id",
    "templateId",
    "entityId",
    "objectId",
    "requestId",
    "files",
    "createdAt",
    "createdBy",
    "updatedAt",
    "updatedBy"
})
@Schema(title = "Данные документа")
public class DocumentRs {

    @NotNull
    @Schema(title = "Идентификатор документа")
    private UUID id;

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

    @Valid
    @Builder.Default
    @ArraySchema(schema = @Schema(title = "Файлы документа"))
    private List<GeneratedFileRs> files = new ArrayList<>();

    @NotNull
    @Schema(title = "Дата и время создания", example = "2026-04-02T10:15:00+03:00")
    private OffsetDateTime createdAt;

    @Schema(title = "Пользователь, создавший документ")
    @Nullable
    private UUID createdBy;

    @NotNull
    @Schema(title = "Дата и время обновления", example = "2026-04-02T10:16:10+03:00")
    private OffsetDateTime updatedAt;

    @Schema(title = "Пользователь, обновивший документ")
    @Nullable
    private UUID updatedBy;
}
