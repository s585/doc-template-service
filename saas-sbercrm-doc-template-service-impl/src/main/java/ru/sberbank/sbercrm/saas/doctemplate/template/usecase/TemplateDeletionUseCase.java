package ru.sberbank.sbercrm.saas.doctemplate.template.usecase;

import java.util.UUID;

public interface TemplateDeletionUseCase {
    void execute(UUID tenantId, UUID userId, UUID templateId);
}
