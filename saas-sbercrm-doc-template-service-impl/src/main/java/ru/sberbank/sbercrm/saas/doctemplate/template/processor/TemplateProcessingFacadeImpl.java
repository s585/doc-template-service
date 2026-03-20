package ru.sberbank.sbercrm.saas.doctemplate.template.processor;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.TemplateFormat;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.TemplateVariableInfo;

@Component
@RequiredArgsConstructor
public class TemplateProcessingFacadeImpl implements TemplateProcessingFacade {

    private final TemplateProcessorResolver templateProcessorResolver;

    @Override
    public List<TemplateVariableInfo> extractVariables(TemplateFormat format, byte[] content) {
        return templateProcessorResolver.resolve(format).extractVariables(content);
    }
}
