package ru.sberbank.sbercrm.doctemplate.template.adapter.filestorage;

import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface FileStorageAdapter {
    FileRs upload(
        UUID tenantId,
        UUID userId,
        String path,
        String description,
        String fileName,
        MultipartFile file
    );

    void ensureFolderExists(UUID tenantId, UUID userId, String path);

    void deleteFile(UUID tenantId, UUID userId, String key);
}
