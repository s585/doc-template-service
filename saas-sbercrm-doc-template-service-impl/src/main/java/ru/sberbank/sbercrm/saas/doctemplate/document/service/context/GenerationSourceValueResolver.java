package ru.sberbank.sbercrm.saas.doctemplate.document.service.context;

import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.sberbank.sbercrm.saas.doctemplate.application.exception.model.BusinessCrmException;
import ru.sberbank.sbercrm.saas.doctemplate.document.constant.DocumentConstants;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.TemplateMapping;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.source.ValueSource;

@Component
@RequiredArgsConstructor
public class GenerationSourceValueResolver {
    private final List<MappingValueResolver> mappingValueResolvers;

    public Object resolve(TemplateMapping mapping, Map<String, Object> sourceObject) {
        if (mapping.getDefinition() == null || mapping.getDefinition().getSource() == null) {
            return "";
        }
        ValueSource source = mapping.getDefinition().getSource();
        return mappingValueResolvers.stream()
            .filter(resolver -> resolver.supports(source))
            .findFirst()
            .map(resolver -> resolver.resolve(mapping, source, sourceObject))
            .orElseThrow(() -> new BusinessCrmException(
                DocumentConstants.ErrorCodes.GENERATION_MAPPING_SOURCE_UNSUPPORTED,
                DocumentConstants.ErrorCodes.GENERATION_MAPPING_SOURCE_UNSUPPORTED,
                mapping.getKey()
            ));
    }
}
