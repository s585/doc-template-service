package ru.sberbank.sbercrm.saas.doctemplate.template.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Метаданные расположения маппинга, вычисленные при импорте шаблона. Только для чтения")
public class TemplateMappingLayoutDto {

    @Schema(description = "Типы источников данных, которые можно выбрать для переменной в этом расположении шаблона")
    private List<String> allowedSourceKinds;
}
