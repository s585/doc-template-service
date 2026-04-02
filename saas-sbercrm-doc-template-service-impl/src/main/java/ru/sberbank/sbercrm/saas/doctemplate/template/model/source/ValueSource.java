package ru.sberbank.sbercrm.saas.doctemplate.template.model.source;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import ru.sberbank.sbercrm.saas.doctemplate.template.constant.TemplateApiConstants;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "kind")
@JsonSubTypes({
    @JsonSubTypes.Type(value = DirectValueSource.class, name = TemplateApiConstants.ValueSourceJsonKinds.DIRECT),
    @JsonSubTypes.Type(value = ReferenceValueSource.class, name = TemplateApiConstants.ValueSourceJsonKinds.REFERENCE),
    @JsonSubTypes.Type(value = ConstantValueSource.class, name = TemplateApiConstants.ValueSourceJsonKinds.CONSTANT)
})
public sealed interface ValueSource permits DirectValueSource, ReferenceValueSource, ConstantValueSource {
    @JsonProperty("kind")
    ValueSourceKind getKind();
}
