package ru.sberbank.sbercrm.saas.doctemplate.application.integration.gateway;

import org.springframework.web.multipart.MultipartFile;
import ru.sberbank.sbercrm.saas.doctemplate.application.integration.client.FileRs;

import java.io.File;
import java.util.UUID;

public interface FileStorageGateway {
    FileRs upload(
        UUID tenantId,
        UUID userId,
        String path,
        String description,
        MultipartFile file
    );

    File download(
        UUID tenantId,
        UUID userId,
        String key
    );

    void deleteFile(UUID tenantId, UUID userId, String key);
}
