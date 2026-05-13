package ru.sberbank.sbercrm.saas.doctemplate.document.service.context;

import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.TemplateMapping;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.source.DirectValueSource;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.source.ValueSource;

@Component
public class DirectMappingValueResolver implements MappingValueResolver {
    private final GenerationPathResolver generationPathResolver;

    public DirectMappingValueResolver(GenerationPathResolver generationPathResolver) {
        this.generationPathResolver = generationPathResolver;
    }

    @Override
    public boolean supports(ValueSource source) {
        return source instanceof DirectValueSource;
    }

    @Override
    public Object resolve(TemplateMapping mapping, ValueSource source, Map<String, Object> sourceObject, UUID tenantId, UUID userId) {
        DirectValueSource directSource = (DirectValueSource) source;
        if (sourceObject == null) {
            return "";
        }
        Object resolvedValue = generationPathResolver.resolveSourcePath(sourceObject, directSource.getPath(), mapping.getKey());
        return resolvedValue == null ? "" : resolvedValue;
    }
}
