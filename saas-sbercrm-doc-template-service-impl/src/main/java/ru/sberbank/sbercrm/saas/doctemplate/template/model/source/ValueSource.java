package ru.sberbank.sbercrm.saas.doctemplate.template.model.source;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import ru.sberbank.sbercrm.saas.doctemplate.template.constant.TemplateConstants;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "kind")
@JsonSubTypes({
    @JsonSubTypes.Type(value = DirectValueSource.class, name = TemplateConstants.ValueSourceJsonKinds.DIRECT),
    @JsonSubTypes.Type(value = ReferenceValueSource.class, name = TemplateConstants.ValueSourceJsonKinds.REFERENCE),
    @JsonSubTypes.Type(value = ConstantValueSource.class, name = TemplateConstants.ValueSourceJsonKinds.CONSTANT)
})
public sealed interface ValueSource permits DirectValueSource, ReferenceValueSource, ConstantValueSource {
    @JsonProperty("kind")
    ValueSourceKind getKind();
}
