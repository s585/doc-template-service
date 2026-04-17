package ru.sberbank.sbercrm.saas.doctemplate.document.service.context;

import java.util.Map;
import org.springframework.stereotype.Component;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.TemplateMapping;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.source.ConstantValueSource;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.source.ValueSource;

@Component
public class ConstantMappingValueResolver implements MappingValueResolver {
    @Override
    public boolean supports(ValueSource source) {
        return source instanceof ConstantValueSource;
    }

    @Override
    public Object resolve(TemplateMapping mapping, ValueSource source, Map<String, Object> sourceObject) {
        ConstantValueSource constantSource = (ConstantValueSource) source;
        return constantSource.getValue();
    }
}
