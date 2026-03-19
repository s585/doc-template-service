package ru.sberbank.sbercrm.saas.doctemplate.template.gateway.filestorage;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import ru.sberbank.sbercrm.saas.doctemplate.application.exception.CrmErrorCodes;
import ru.sberbank.sbercrm.saas.doctemplate.template.constant.TemplateConstants;
import ru.sberbank.sbercrm.saas.doctemplate.application.exception.model.SystemCrmException;
import ru.sberbank.sbercrm.saas.doctemplate.template.properties.TemplateProperties;

import java.io.IOException;
import java.util.UUID;

@Component
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
}
