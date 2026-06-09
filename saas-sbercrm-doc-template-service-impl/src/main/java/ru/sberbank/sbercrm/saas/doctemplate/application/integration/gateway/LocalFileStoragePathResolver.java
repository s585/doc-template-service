package ru.sberbank.sbercrm.saas.doctemplate.application.integration.gateway;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.GenerationJob;
import ru.sberbank.sbercrm.saas.doctemplate.template.properties.FileStorageProperties;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "saas.doc-template.file-storage.local", name = "enabled", havingValue = "true")
public class LocalFileStoragePathResolver implements FileStoragePathResolver {
    private final FileStorageProperties fileStorageProperties;

    @Override
    public String templateFolder(UUID entityId) {
        return fileStorageProperties.getLocal().getTemplatesFolder() + "/" + entityId;
    }

    @Override
    public String generatedFolder(GenerationJob job) {
        return fileStorageProperties.getLocal().getDocumentsFolder() + "/" + job.getEntityId() + "/" + job.getObjectId() + "/"
            + job.getDocumentId();
    }
}
