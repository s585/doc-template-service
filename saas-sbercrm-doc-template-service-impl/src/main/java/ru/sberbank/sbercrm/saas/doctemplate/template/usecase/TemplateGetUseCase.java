package ru.sberbank.sbercrm.saas.doctemplate.template.usecase;

import ru.sberbank.sbercrm.saas.doctemplate.template.model.Template;

import java.util.UUID;

public interface TemplateGetUseCase {
    Template execute(UUID tenantId, UUID templateId);
}
