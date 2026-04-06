package ru.sberbank.sbercrm.saas.doctemplate.application.integration.gateway;

import feign.FeignException;
import feign.Response;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import ru.sberbank.sbercrm.saas.doctemplate.application.exception.CrmErrorCodes;
import ru.sberbank.sbercrm.saas.doctemplate.application.integration.client.FileRq;
import ru.sberbank.sbercrm.saas.doctemplate.application.integration.client.FileRs;
import ru.sberbank.sbercrm.saas.doctemplate.application.integration.client.FileStorageClient;
import ru.sberbank.sbercrm.saas.doctemplate.application.exception.model.SystemCrmException;
import ru.sberbank.sbercrm.saas.doctemplate.template.properties.TemplateProperties;

@Component
@ConditionalOnProperty(prefix = "saas.doc-template.file-storage", name = "stub-enabled", havingValue = "false", matchIfMissing = true)
@RequiredArgsConstructor
public class FeignFileStorageGateway implements FileStorageGateway {
    private final FileStorageClient fileStorageClient;
    private final TemplateProperties templateProperties;

    @Override
    public FileRs upload(
        UUID tenantId,
        UUID userId,
        String path,
        String description,
        MultipartFile file
    ) {
        try {
            return fileStorageClient.upload(
                templateProperties.getFileStorage().getSource(),
                FileRq.builder()
                    .path(path)
                    .source(templateProperties.getFileStorage().getSource())
                    .description(description)
                    .build(),
                file,
                tenantId,
                userId
            );
        } catch (FeignException | IOException ex) {
            throw new SystemCrmException(ex, CrmErrorCodes.FILE_STORAGE_REQUEST_FAILED, path);
        }
    }

    @Override
    public void deleteFile(UUID tenantId, UUID userId, String key) {
        try {
            fileStorageClient.deleteFile(templateProperties.getFileStorage().getSource(), key, tenantId, userId);
        } catch (FeignException.NotFound ex) {
            // Delete is idempotent for file storage: missing file must not block template deletion.
        } catch (FeignException ex) {
            throw new SystemCrmException(ex, CrmErrorCodes.FILE_STORAGE_REQUEST_FAILED, key);
        }
    }

    @Override
    public File download(UUID tenantId, UUID userId, String key) {
        try (Response response = fileStorageClient.download(
                templateProperties.getFileStorage().getSource(),
                key,
                tenantId,
                userId
            )) {
            return materializeDownloadedFile(response, key);
        } catch (FeignException | IOException ex ) {
            throw new SystemCrmException(ex, CrmErrorCodes.FILE_STORAGE_REQUEST_FAILED, key);
        }
    }

    private File materializeDownloadedFile(Response response, String key) {
        if (response.body() == null) {
            throw new SystemCrmException(CrmErrorCodes.FILE_STORAGE_REQUEST_FAILED, key);
        }
        try {
            File tempFile = File.createTempFile("doc-template-download-", extractExtension(key));
            tempFile.deleteOnExit();
            try (InputStream inputStream = response.body().asInputStream()) {
                Files.copy(inputStream, tempFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
            return tempFile;
        } catch (IOException ex) {
            throw new SystemCrmException(ex, CrmErrorCodes.FILE_STORAGE_REQUEST_FAILED, key);
        }
    }

    private String extractExtension(String key) {
        int extensionIndex = key.lastIndexOf('.');
        if (extensionIndex < 0) {
            return ".tmp";
        }
        return key.substring(extensionIndex);
    }
}
