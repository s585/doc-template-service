package ru.sberbank.sbercrm.saas.doctemplate.template.processor;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.GenerationTemplateContext;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.TemplateFormat;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.TemplateVariableInfo;

@Component
@Slf4j
@RequiredArgsConstructor
public class TemplateProcessingFacadeImpl implements TemplateProcessingFacade {

    private final TemplateProcessorResolver templateProcessorResolver;

    @Override
    public List<TemplateVariableInfo> extractVariables(TemplateFormat format, byte[] content) {
        FormatAwareTemplateProcessor processor = templateProcessorResolver.resolve(format);
        log.debug(
            "Extracting template variables: format={}, contentSizeBytes={}, processor={}",
            format,
            content.length,
            processor.getClass().getSimpleName()
        );
        List<TemplateVariableInfo> variables = processor.extractVariables(content);
        log.debug(
            "Extracted template variables: format={}, contentSizeBytes={}, processor={}, variableCount={}",
            format,
            content.length,
            processor.getClass().getSimpleName(),
            variables.size()
        );
        return variables;
    }

    @Override
    public byte[] generate(TemplateFormat format, byte[] content, GenerationTemplateContext context) {
        FormatAwareTemplateProcessor processor = templateProcessorResolver.resolve(format);
        log.debug(
            "Generating document from template: format={}, templateSizeBytes={}, processor={}, "
                + "scalarKeys={}, collections={}",
            format,
            content.length,
            processor.getClass().getSimpleName(),
            context.getScalarValues().keySet(),
            describeCollections(context)
        );
        byte[] generatedContent = processor.generate(content, context);
        log.debug(
            "Generated document from template: format={}, generatedSizeBytes={}, processor={}",
            format,
            generatedContent.length,
            processor.getClass().getSimpleName()
        );
        return generatedContent;
    }

    private List<String> describeCollections(GenerationTemplateContext context) {
        return context.getCollections().stream()
            .map(dataset -> "keys=" + dataset.getKeys() + ", rowCount=" + dataset.getRows().size())
            .toList();
    }
}
