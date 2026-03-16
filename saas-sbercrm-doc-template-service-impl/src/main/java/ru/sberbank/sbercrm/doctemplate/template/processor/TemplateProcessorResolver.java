package ru.sberbank.sbercrm.doctemplate.template.processor;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.sberbank.sbercrm.doctemplate.common.constant.CrmErrorCodes;
import ru.sberbank.sbercrm.doctemplate.common.exception.SystemCrmException;
import ru.sberbank.sbercrm.doctemplate.template.model.TemplateFormat;

@Component
@RequiredArgsConstructor
public class TemplateProcessorResolver {

    private final List<FormatAwareTemplateProcessor> processors;

    public FormatAwareTemplateProcessor resolve(TemplateFormat format) {
        return processors.stream()
            .filter(processor -> processor.supports(format))
            .findFirst()
            .orElseThrow(() -> new SystemCrmException(CrmErrorCodes.TEMPLATE_PROCESSOR_MISSING, new Object[]{format}));
    }
}
