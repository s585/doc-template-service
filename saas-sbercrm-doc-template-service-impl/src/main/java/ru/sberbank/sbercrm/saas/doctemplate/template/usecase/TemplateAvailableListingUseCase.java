package ru.sberbank.sbercrm.saas.doctemplate.template.usecase;

import java.util.UUID;
import java.util.List;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.Template;

public interface TemplateAvailableListingUseCase {
    List<Template> execute(UUID tenantId, UUID userId, UUID entityId, UUID objectId);
}
