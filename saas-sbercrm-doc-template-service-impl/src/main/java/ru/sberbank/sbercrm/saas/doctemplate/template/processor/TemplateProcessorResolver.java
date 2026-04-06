package ru.sberbank.sbercrm.saas.doctemplate.template.processor;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import ru.sberbank.sbercrm.saas.doctemplate.application.exception.model.SystemCrmException;
import ru.sberbank.sbercrm.saas.doctemplate.template.constant.TemplateConstants;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.TemplateFormat;

@Component
public class TemplateProcessorResolver {

    private final Map<TemplateFormat, FormatAwareTemplateProcessor> formatToProcessor;

    public TemplateProcessorResolver(List<FormatAwareTemplateProcessor> processors) {
        this.formatToProcessor = Arrays.stream(TemplateFormat.values())
            .collect(Collectors.toMap(
                Function.identity(),
                format -> resolveProcessor(processors, format),
                (left, right) -> left,
                () -> new EnumMap<>(TemplateFormat.class)
            ));
    }

    public FormatAwareTemplateProcessor resolve(TemplateFormat format) {
        return formatToProcessor.get(format);
    }

    private FormatAwareTemplateProcessor resolveProcessor(
        List<FormatAwareTemplateProcessor> processors,
        TemplateFormat format
    ) {
        List<FormatAwareTemplateProcessor> supportedProcessors = processors.stream()
            .filter(candidate -> candidate.supports(format))
            .toList();
        if (supportedProcessors.isEmpty()) {
            throw new SystemCrmException(TemplateConstants.ErrorCodes.TEMPLATE_PROCESSOR_MISSING, format.value());
        }
        if (supportedProcessors.size() > 1) {
            throw new SystemCrmException(TemplateConstants.ErrorCodes.TEMPLATE_PROCESSOR_DUPLICATE, format.value());
        }
        return supportedProcessors.getFirst();
    }
}
