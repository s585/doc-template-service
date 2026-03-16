package ru.sberbank.sbercrm.doctemplate.template.processor;

import java.util.List;
import ru.sberbank.sbercrm.doctemplate.template.model.TemplateFormat;
import ru.sberbank.sbercrm.doctemplate.template.model.TemplateVariableInfo;

public interface TemplateProcessingFacade {

    List<TemplateVariableInfo> extractVariables(TemplateFormat format, byte[] content);
}
