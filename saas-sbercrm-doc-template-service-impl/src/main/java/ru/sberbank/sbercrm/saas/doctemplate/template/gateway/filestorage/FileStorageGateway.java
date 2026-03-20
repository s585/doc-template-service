package ru.sberbank.sbercrm.saas.doctemplate.template.gateway.filestorage;

import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface FileStorageGateway {
    FileRs upload(
        UUID tenantId,
        UUID userId,
        String path,
        String description,
        MultipartFile file
    );

    void deleteFile(UUID tenantId, UUID userId, String key);
}
