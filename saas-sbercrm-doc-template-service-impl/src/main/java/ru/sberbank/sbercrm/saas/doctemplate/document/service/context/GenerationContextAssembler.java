package ru.sberbank.sbercrm.saas.doctemplate.document.service.context;

import java.util.UUID;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.GenerationJob;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.GenerationTemplateContext;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.Template;

public interface GenerationContextAssembler {
    GenerationTemplateContext assemble(GenerationJob job, UUID userId, Template template);
}
