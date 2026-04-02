package ru.sberbank.sbercrm.saas.doctemplate.document.usecase;

import java.util.UUID;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.Document;

public interface DocumentGetUseCase {
    Document execute(UUID tenantId, UUID documentId);
}
