package ru.sberbank.sbercrm.saas.doctemplate.application.integration.gateway;

import java.util.UUID;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.GenerationJob;

public interface FileStoragePathResolver {
    String templateFolder(UUID entityId);

    String generatedFolder(GenerationJob job);
}
