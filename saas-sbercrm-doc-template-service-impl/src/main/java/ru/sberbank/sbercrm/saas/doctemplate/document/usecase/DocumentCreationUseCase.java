package ru.sberbank.sbercrm.saas.doctemplate.document.usecase;

import java.util.UUID;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.Document;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.DocumentCreationCmd;

public interface DocumentCreationUseCase {
    Document execute(UUID tenantId, UUID userId, DocumentCreationCmd command);
}
