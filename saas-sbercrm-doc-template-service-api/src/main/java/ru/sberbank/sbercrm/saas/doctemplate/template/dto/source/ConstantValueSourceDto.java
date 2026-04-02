package ru.sberbank.sbercrm.saas.doctemplate.template.dto.source;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.sberbank.sbercrm.saas.doctemplate.template.constant.TemplateApiConstants;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@JsonTypeName(TemplateApiConstants.ValueSourceJsonKinds.CONSTANT)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder({"kind", "value"})
@Schema(description = "Источник значения в виде константы")
public class ConstantValueSourceDto implements ValueSourceDto {

    @Schema(description = "Константное значение", example = "2026-03-10")
    private Object value;

    @Override
    public String getKind() {
        return TemplateApiConstants.ValueSourceJsonKinds.CONSTANT;
    }
}
