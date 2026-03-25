package ru.sberbank.sbercrm.doctemplate.template.dto.source;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import ru.sberbank.sbercrm.doctemplate.template.constant.TemplateApiConstants;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "kind")
@JsonSubTypes({
    @JsonSubTypes.Type(value = DirectValueSourceDto.class, name = TemplateApiConstants.ValueSourceJsonKinds.DIRECT),
    @JsonSubTypes.Type(value = ReferenceValueSourceDto.class, name = TemplateApiConstants.ValueSourceJsonKinds.REFERENCE),
    @JsonSubTypes.Type(value = ConstantValueSourceDto.class, name = TemplateApiConstants.ValueSourceJsonKinds.CONSTANT)
})
public interface ValueSourceDto {
    @Schema(description = "Вид источника значения")
    @JsonProperty("kind")
    String getKind();
}
