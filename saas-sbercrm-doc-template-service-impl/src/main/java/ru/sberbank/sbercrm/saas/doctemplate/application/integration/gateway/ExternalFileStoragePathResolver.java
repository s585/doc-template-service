package ru.sberbank.sbercrm.saas.doctemplate.application.integration.gateway;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.GenerationJob;
import ru.sberbank.sbercrm.saas.doctemplate.template.properties.FileStorageProperties;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
    prefix = "saas.doc-template.file-storage.local",
    name = "enabled",
    havingValue = "false",
    matchIfMissing = true
)
public class ExternalFileStoragePathResolver implements FileStoragePathResolver {
    private static final String GENERATED_FOLDER = "generated";

    private final FileStorageProperties fileStorageProperties;

    @Override
    public String templateFolder(UUID entityId) {
        return fileStorageProperties.getExternal().getFolder() + "/" + entityId;
    }

    @Override
    public String generatedFolder(GenerationJob job) {
        return fileStorageProperties.getExternal().getFolder() + "/" + GENERATED_FOLDER + "/" + job.getEntityId() + "/"
            + job.getObjectId() + "/" + job.getDocumentId();
    }
}
