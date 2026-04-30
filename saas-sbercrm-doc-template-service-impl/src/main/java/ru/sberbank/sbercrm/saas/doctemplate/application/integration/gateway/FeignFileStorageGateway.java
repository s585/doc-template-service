package ru.sberbank.sbercrm.saas.doctemplate.application.integration.gateway;

import feign.FeignException;
import feign.Response;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import ru.sberbank.sbercrm.saas.doctemplate.application.exception.CrmErrorCodes;
import ru.sberbank.sbercrm.saas.doctemplate.application.integration.client.FileFilterRq;
import ru.sberbank.sbercrm.saas.doctemplate.application.integration.client.FileRq;
import ru.sberbank.sbercrm.saas.doctemplate.application.integration.client.FileRs;
import ru.sberbank.sbercrm.saas.doctemplate.application.integration.client.FileStorageClient;
import ru.sberbank.sbercrm.saas.doctemplate.application.exception.model.SystemCrmException;
import ru.sberbank.sbercrm.saas.doctemplate.template.properties.DocTemplateProperties;

@Component
@ConditionalOnProperty(prefix = "saas.doc-template.file-storage", name = "stub-enabled", havingValue = "false", matchIfMissing = true)
@RequiredArgsConstructor
public class FeignFileStorageGateway implements FileStorageGateway {
    private final FileStorageClient fileStorageClient;
    private final DocTemplateProperties docTemplateProperties;

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
                docTemplateProperties.getFileStorage().getSource(),
                FileRq.builder()
                    .path(path)
                    .source(docTemplateProperties.getFileStorage().getSource())
                    .description(description)
                    .build(),
                file,
                tenantId,
                userId
            );
        } catch (FeignException | IOException ex) {
            throw new SystemCrmException(CrmErrorCodes.FILE_STORAGE_REQUEST_FAILED, CrmErrorCodes.FILE_STORAGE_REQUEST_FAILED, ex, path);
        }
    }

    @Override
    public void deleteFile(UUID tenantId, UUID userId, String key) {
        try {
            fileStorageClient.deleteFile(docTemplateProperties.getFileStorage().getSource(), key, tenantId, userId);
        } catch (FeignException.NotFound ex) {
            // Delete is idempotent for file storage: missing file must not block template deletion.
        } catch (FeignException ex) {
            throw new SystemCrmException(CrmErrorCodes.FILE_STORAGE_REQUEST_FAILED, CrmErrorCodes.FILE_STORAGE_REQUEST_FAILED, ex, key);
        }
    }

    @Override
    public byte[] download(UUID tenantId, UUID userId, String key) {
        try {
            Response response = fileStorageClient.download(
                docTemplateProperties.getFileStorage().getSource(),
                key,
                tenantId,
                userId
            );
            return readDownloadedContent(response, key);
        } catch (FeignException ex) {
            throw new SystemCrmException(
                CrmErrorCodes.FILE_STORAGE_REQUEST_FAILED,
                CrmErrorCodes.FILE_STORAGE_REQUEST_FAILED,
                ex,
                key
            );
        }
    }

    @Override
    public List<FileRs> findAllByFilter(UUID tenantId, UUID userId, FileFilterRq filter) {
        try {
            return fileStorageClient.getWithFilter(
                docTemplateProperties.getFileStorage().getSource(),
                filter,
                tenantId,
                userId
            );
        } catch (FeignException ex) {
            throw new SystemCrmException(
                CrmErrorCodes.FILE_STORAGE_REQUEST_FAILED,
                CrmErrorCodes.FILE_STORAGE_REQUEST_FAILED,
                ex,
                filter.getPrefixKey()
            );
        }
    }

    private byte[] readDownloadedContent(Response response, String key) {
        if (response.body() == null) {
            throw new SystemCrmException(
                CrmErrorCodes.FILE_STORAGE_REQUEST_FAILED,
                CrmErrorCodes.FILE_STORAGE_REQUEST_FAILED,
                key
            );
        }
        try {
            try (Response ignored = response; InputStream inputStream = response.body().asInputStream()) {
                return inputStream.readAllBytes();
            }
        } catch (IOException ex) {
            throw new SystemCrmException(
                CrmErrorCodes.FILE_STORAGE_REQUEST_FAILED,
                CrmErrorCodes.FILE_STORAGE_REQUEST_FAILED,
                ex,
                key
            );
        }
    }

}
