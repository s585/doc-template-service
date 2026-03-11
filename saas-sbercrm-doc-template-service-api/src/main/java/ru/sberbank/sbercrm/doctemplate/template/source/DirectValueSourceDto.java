package ru.sberbank.sbercrm.doctemplate.template.source;

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

import java.io.Serializable;

import static ru.sberbank.sbercrm.doctemplate.template.source.ValueSourceDto.SourceKind.DIRECT;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@JsonTypeName(DIRECT)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder({"kind", "path"})
@Schema(description = "Источник значения, которое читается напрямую из исходного объекта")
public class DirectValueSourceDto implements ValueSourceDto, Serializable {
    private static final long serialVersionUID = -2379362295802904914L;

    @NotBlank
    @Schema(description = "Путь до значения в исходном объекте", example = "source.doc_number")
    private String path;

    @Override
    public String getKind() {
        return DIRECT;
    }
}
