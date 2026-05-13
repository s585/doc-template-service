package ru.sberbank.sbercrm.saas.doctemplate.document.service.context;

import java.util.Map;
import java.util.UUID;
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
    public Object resolve(TemplateMapping mapping, ValueSource source, Map<String, Object> sourceObject, UUID tenantId, UUID userId) {
        ConstantValueSource constantSource = (ConstantValueSource) source;
        return constantSource.getValue();
    }
}
