package ru.sberbank.sbercrm.saas.doctemplate.template.dto.source;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.sberbank.sbercrm.saas.doctemplate.template.constant.TemplateApiConstants;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@JsonTypeName(TemplateApiConstants.ValueSourceJsonKinds.DIRECT)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder({"kind", "path"})
@Schema(description = "Источник значения, которое читается напрямую из исходного объекта")
public class DirectValueSourceDto implements ValueSourceDto {

    @NotBlank
    @Schema(description = "Путь до значения в исходном объекте", example = "source.doc_number")
    private String path;

    @Override
    public String getKind() {
        return TemplateApiConstants.ValueSourceJsonKinds.DIRECT;
    }
}
