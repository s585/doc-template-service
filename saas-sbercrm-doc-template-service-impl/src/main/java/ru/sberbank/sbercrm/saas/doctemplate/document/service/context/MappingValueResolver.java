package ru.sberbank.sbercrm.saas.doctemplate.document.service.context;

import java.util.Map;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.TemplateMapping;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.source.ValueSource;

public interface MappingValueResolver {
    boolean supports(ValueSource source);

    Object resolve(TemplateMapping mapping, ValueSource source, Map<String, Object> sourceObject);
}
