package ru.sberbank.sbercrm.doctemplate.template;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.checkerframework.checker.nullness.qual.Nullable;
import ru.sberbank.sbercrm.doctemplate.rule.RuleDto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder({"name", "description", "displayCondition", "active", "mappings"})
@Schema(description = "Запрос на полное обновление шаблона")
public class TemplateUpdateRq implements Serializable {
    private static final long serialVersionUID = 4640994087951262490L;

    @NotBlank
    @Schema(description = "Название шаблона")
    private String name;

    @Schema(description = "Описание шаблона")
    private String description;

    @Valid
    @Schema(description = "Условие отображения шаблона")
    private RuleDto displayCondition;

    @NotNull
    @Schema(description = "Признак активности шаблона")
    private Boolean active;

    @Valid
    @Builder.Default
    @NotNull
    @ArraySchema(schema = @Schema(description = "Маппинги переменных шаблона"))
    private List<TemplateMappingDto> mappings = new ArrayList<>();
}
