package ru.sberbank.sbercrm.saas.doctemplate.template.processor;

import java.util.List;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.TemplateFormat;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.TemplateVariableInfo;

public interface TemplateProcessingFacade {

    List<TemplateVariableInfo> extractVariables(TemplateFormat format, byte[] content);
}
