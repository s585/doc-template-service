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
@Schema(description = "Read-only template mapping layout metadata calculated from the imported template file")
public class TemplateMappingLayoutDto {

    @Schema(description = "Source kinds that can be selected for this variable in its template layout")
    private List<String> allowedSourceKinds;
}
