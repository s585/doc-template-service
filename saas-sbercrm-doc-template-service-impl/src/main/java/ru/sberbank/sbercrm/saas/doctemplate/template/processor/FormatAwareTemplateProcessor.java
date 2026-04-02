package ru.sberbank.sbercrm.saas.doctemplate.template.processor;

import java.util.Map;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.TemplateFormat;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.TemplateVariableInfo;

import java.util.List;

public interface FormatAwareTemplateProcessor {
    boolean supports(TemplateFormat format);

    List<TemplateVariableInfo> extractVariables(byte[] content);

    byte[] generate(byte[] content, Map<String, String> values);
}
