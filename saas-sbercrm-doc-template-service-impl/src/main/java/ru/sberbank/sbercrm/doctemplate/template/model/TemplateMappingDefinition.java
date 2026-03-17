package ru.sberbank.sbercrm.doctemplate.template.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.sberbank.sbercrm.doctemplate.template.model.expression.Expression;
import ru.sberbank.sbercrm.doctemplate.template.model.source.ValueSource;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder({"scope", "type", "expression", "source"})
public class TemplateMappingDefinition {
    private MappingScope scope;
    private TemplateValueType type;
    private Expression expression;
    private ValueSource source;
}
