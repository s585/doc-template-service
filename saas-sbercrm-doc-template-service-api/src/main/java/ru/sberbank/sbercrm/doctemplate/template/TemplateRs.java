package ru.sberbank.sbercrm.doctemplate.template;

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
import ru.sberbank.sbercrm.doctemplate.rule.RuleDto;

import java.io.Serializable;
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
    "name",
    "code",
    "format",
    "description",
    "s3Key",
    "displayCondition",
    "entityId",
    "active",
    "templateMapping",
    "createdAt",
    "createdBy",
    "updatedAt",
    "updatedBy"
})
@Schema(description = "Ответ с данными шаблона")
public class TemplateRs implements Serializable {
    private static final long serialVersionUID = -427694471935565515L;

    @NotNull
    @Schema(description = "Идентификатор шаблона")
    private UUID id;

    @NotNull
    @Schema(description = "Наименование шаблона")
    private String name;

    @NotNull
    @Schema(description = "Системный код шаблона")
    private String code;

    @NotNull
    @Schema(description = "Формат файла шаблона")
    private String format;

    @Schema(description = "Описание шаблона")
    private String description;

    @NotNull
    @Schema(description = "Ключ файла шаблона в S3")
    private String s3Key;

    @Valid
    @Schema(description = "Условие отображения шаблона")
    private RuleDto displayCondition;

    @NotNull
    @Schema(description = "Идентификатор сущности, к которой относится шаблон")
    private UUID entityId;

    @NotNull
    @Schema(description = "Признак активности шаблона")
    private Boolean active;

    @Valid
    @Builder.Default
    @ArraySchema(schema = @Schema(description = "Маппинги переменных шаблона"))
    private List<TemplateMappingDto> templateMapping = new ArrayList<>();

    @NotNull
    @Schema(description = "Дата и время создания шаблона", example = "2026-03-10T12:30:00+03:00")
    private OffsetDateTime createdAt;

    @Schema(description = "Пользователь, создавший шаблон")
    private UUID createdBy;

    @NotNull
    @Schema(description = "Дата и время последнего обновления шаблона", example = "2026-03-10T12:45:00+03:00")
    private OffsetDateTime updatedAt;

    @Schema(description = "Пользователь, обновивший шаблон")
    private UUID updatedBy;
}
