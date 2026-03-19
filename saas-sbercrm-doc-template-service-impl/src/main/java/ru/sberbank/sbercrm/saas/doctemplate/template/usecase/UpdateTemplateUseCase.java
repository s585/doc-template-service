package ru.sberbank.sbercrm.saas.doctemplate.template.usecase;

import ru.sberbank.sbercrm.saas.doctemplate.template.model.Template;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.TemplateUpdateCmd;

import java.util.UUID;

public interface UpdateTemplateUseCase {
    Template execute(UUID tenantId, UUID userId, UUID templateId, TemplateUpdateCmd request);
}
