package ru.sberbank.sbercrm.saas.doctemplate.application.integration.gateway;

import java.util.List;
import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;
import ru.sberbank.sbercrm.saas.doctemplate.application.integration.client.FileFilterRq;
import ru.sberbank.sbercrm.saas.doctemplate.application.integration.client.FileRs;

public interface FileStorageGateway {
    FileRs upload(
        UUID tenantId,
        UUID userId,
        String path,
        String description,
        MultipartFile file
    );

    byte[] download(
        UUID tenantId,
        UUID userId,
        String key
    );

    List<FileRs> findAllByFilter(
        UUID tenantId,
        UUID userId,
        FileFilterRq filter
    );

    void deleteFile(UUID tenantId, UUID userId, String key);
}
