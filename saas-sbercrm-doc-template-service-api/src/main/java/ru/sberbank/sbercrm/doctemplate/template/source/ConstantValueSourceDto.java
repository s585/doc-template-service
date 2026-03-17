package ru.sberbank.sbercrm.doctemplate.template.source;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

import static ru.sberbank.sbercrm.doctemplate.template.source.ValueSourceDto.SourceKind.CONSTANT;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@JsonTypeName(CONSTANT)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder({"kind", "value"})
@Schema(description = "Источник значения в виде константы")
public class ConstantValueSourceDto implements ValueSourceDto, Serializable {
    private static final long serialVersionUID = 4712066924486506163L;

    @Schema(description = "Константное значение", example = "2026-03-10")
    private Object value;

    @Override
    public String getKind() {
        return CONSTANT;
    }
}
