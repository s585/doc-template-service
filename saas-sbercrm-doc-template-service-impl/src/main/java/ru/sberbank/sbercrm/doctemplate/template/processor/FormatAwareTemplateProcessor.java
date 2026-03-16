package ru.sberbank.sbercrm.doctemplate.template.processor;

import ru.sberbank.sbercrm.doctemplate.template.model.TemplateFormat;
import ru.sberbank.sbercrm.doctemplate.template.model.TemplateVariableInfo;

import java.util.List;

public interface FormatAwareTemplateProcessor {
    boolean supports(TemplateFormat format);

    List<TemplateVariableInfo> extractVariables(byte[] content);
}
