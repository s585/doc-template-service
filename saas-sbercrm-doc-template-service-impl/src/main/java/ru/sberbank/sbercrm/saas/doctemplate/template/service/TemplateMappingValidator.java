package ru.sberbank.sbercrm.saas.doctemplate.template.service;

import java.util.List;
import org.springframework.stereotype.Component;
import ru.sberbank.sbercrm.saas.doctemplate.application.exception.model.BusinessCrmException;
import ru.sberbank.sbercrm.saas.doctemplate.template.constant.TemplateConstants;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.MappingScope;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.TemplateMapping;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.source.ReferenceValueSource;

@Component
public class TemplateMappingValidator {
    public void validate(List<TemplateMapping> mappings) {
        if (mappings == null) {
            return;
        }
        mappings.stream()
            .filter(this::isInvalid)
            .findFirst()
            .ifPresent(mapping -> {
                throw new BusinessCrmException(
                    TemplateConstants.ErrorCodes.TEMPLATE_VARIABLE_INVALID,
                    TemplateConstants.ErrorCodes.TEMPLATE_VARIABLE_INVALID,
                    mapping.getKey()
                );
            });
    }

    private boolean isInvalid(TemplateMapping mapping) {
        if (mapping == null || mapping.getDefinition() == null) {
            return false;
        }
        if (TemplateConstants.MappingKeys.GENERATED_FILE_NAME.equals(mapping.getKey())) {
            return mapping.getDefinition().getScope() != MappingScope.FILE_NAME
                || mapping.getDefinition().getSource() instanceof ReferenceValueSource;
        }
        if (mapping.getDefinition().getScope() == MappingScope.COLLECTION) {
            return mapping.getDefinition().getSource() != null
                && !(mapping.getDefinition().getSource() instanceof ReferenceValueSource);
        }
        return mapping.getDefinition().getSource() instanceof ReferenceValueSource
            && mapping.getDefinition().getScope() != MappingScope.COLLECTION;
    }
}
