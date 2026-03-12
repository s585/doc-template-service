package ru.sberbank.sbercrm.doctemplate.template.model.source;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "kind")
@JsonSubTypes({
    @JsonSubTypes.Type(value = DirectValueSource.class, name = ValueSourceJsonKinds.DIRECT),
    @JsonSubTypes.Type(value = ReferenceValueSource.class, name = ValueSourceJsonKinds.REFERENCE),
    @JsonSubTypes.Type(value = ConstantValueSource.class, name = ValueSourceJsonKinds.CONSTANT)
})
public sealed interface ValueSource permits DirectValueSource, ReferenceValueSource, ConstantValueSource {
    @JsonProperty("kind")
    ValueSourceKind getKind();
}
