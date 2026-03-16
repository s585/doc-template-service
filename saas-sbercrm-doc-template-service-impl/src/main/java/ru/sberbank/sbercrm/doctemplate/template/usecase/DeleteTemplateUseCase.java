package ru.sberbank.sbercrm.doctemplate.template.usecase;

import java.util.UUID;

public interface DeleteTemplateUseCase {
    void execute(UUID tenantId, UUID userId, UUID templateId);
}
