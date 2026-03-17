package ru.sberbank.sbercrm.doctemplate.template.adapter.filestorage;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import ru.sberbank.sbercrm.doctemplate.template.config.TemplateProperties;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class FeignFileStorageAdapter implements FileStorageAdapter {
    private final FileStorageClient fileStorageClient;
    private final TemplateProperties templateProperties;

    @Override
    public FileRs upload(
        UUID tenantId,
        UUID userId,
        String path,
        String description,
        String fileName,
        MultipartFile file
    ) {
        return fileStorageClient.upload(
            templateProperties.getFileStorage().getSource(),
            FileRq.builder()
                .path(path)
                .source(templateProperties.getFileStorage().getSource())
                .description(description)
                .fileName(fileName)
                .build(),
            file,
            tenantId,
            userId
        );
    }

    @Override
    public void ensureFolderExists(UUID tenantId, UUID userId, String path) {
        try {
            fileStorageClient.getFolder(templateProperties.getFileStorage().getSource(), path, tenantId, userId);
        } catch (FeignException.NotFound ex) {
            fileStorageClient.createFolder(
                templateProperties.getFileStorage().getSource(),
                FolderRq.builder()
                    .path(path)
                    .source(templateProperties.getFileStorage().getSource())
                    .build(),
                tenantId,
                userId
            );
        }
    }

    @Override
    public void deleteFile(UUID tenantId, UUID userId, String key) {
        try {
            fileStorageClient.deleteFile(templateProperties.getFileStorage().getSource(), key, tenantId, userId);
        } catch (FeignException.NotFound ex) {
            // Delete is idempotent for file storage: missing file must not block template deletion.
        }
    }
}
