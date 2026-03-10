package ru.sberbank.sbercrm.doctemplate.template.source;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "kind")
@JsonSubTypes({
    @JsonSubTypes.Type(value = DirectSourceDto.class, name = SourceDto.SourceKind.DIRECT),
    @JsonSubTypes.Type(value = ReferenceSourceDto.class, name = SourceDto.SourceKind.REFERENCE),
    @JsonSubTypes.Type(value = ConstantSourceDto.class, name = SourceDto.SourceKind.CONSTANT)
})
public interface SourceDto {
    @Schema(description = "Вид источника значения")
    @JsonProperty("kind")
    String getKind();

    final class SourceKind {
        public static final String DIRECT = "DIRECT";
        public static final String REFERENCE = "REFERENCE";
        public static final String CONSTANT = "CONSTANT";

        private SourceKind() {
        }
    }
}
