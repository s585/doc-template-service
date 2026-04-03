package ru.sberbank.sbercrm.saas.doctemplate.application.integration.gateway;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import ru.sberbank.sbercrm.saas.doctemplate.application.exception.CrmErrorCodes;
import ru.sberbank.sbercrm.saas.doctemplate.application.exception.model.SystemCrmException;
import ru.sberbank.sbercrm.saas.doctemplate.application.integration.client.FileRs;
import ru.sberbank.sbercrm.saas.doctemplate.template.properties.TemplateProperties;
import ru.sberbank.sbercrm.saas.doctemplate.template.util.TemplateFileUtils;

@Component
@ConditionalOnProperty(prefix = "saas.doc-template.file-storage", name = "stub-enabled", havingValue = "true")
@RequiredArgsConstructor
public class FileStorageGatewayStub implements FileStorageGateway {
    private final TemplateProperties templateProperties;

    @Override
    public FileRs upload(UUID tenantId, UUID userId, String path, String description, MultipartFile file) {
        try {
            String fileName = TemplateFileUtils.resolveOriginalFileName(file);
            String key = buildKey(path, fileName);
            Path targetPath = resolveStoragePath(key);

            Files.createDirectories(targetPath.getParent());
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }

            return buildResponse(key, path, targetPath, fileName);
        } catch (IOException ex) {
            throw new SystemCrmException(ex, CrmErrorCodes.FILE_STORAGE_REQUEST_FAILED, path);
        }
    }

    @Override
    public FileRs download(UUID tenantId, UUID userId, String key) {
        try {
            Path targetPath = resolveStoragePath(key);
            if (!Files.exists(targetPath) || !Files.isRegularFile(targetPath)) {
                throw new IOException("File not found by key: " + key);
            }

            String fileName = targetPath.getFileName().toString();
            String path = extractPathFromKey(key);
            return buildResponse(key, path, targetPath, fileName);
        } catch (IOException ex) {
            throw new SystemCrmException(ex, CrmErrorCodes.FILE_STORAGE_REQUEST_FAILED, key);
        }
    }

    @Override
    public void deleteFile(UUID tenantId, UUID userId, String key) {
        try {
            Files.deleteIfExists(resolveStoragePath(key));
        } catch (IOException ex) {
            throw new SystemCrmException(ex, CrmErrorCodes.FILE_STORAGE_REQUEST_FAILED, key);
        }
    }

    private FileRs buildResponse(String key, String path, Path targetPath, String fileName) {
        return FileRs.builder()
            .key(key)
            .path(targetPath.toAbsolutePath().toString())
            .source(templateProperties.getFileStorage().getSource())
            .fileName(fileName)
            .build();
    }

    private Path resolveStoragePath(String key) {
        return Path.of(templateProperties.getFileStorage().getStubRootPath())
            .resolve(normalizeRelativePath(key))
            .normalize();
    }

    private String buildKey(String path, String fileName) {
        String normalizedPath = normalizeRelativePath(path);
        String sanitizedFileName = Path.of(fileName).getFileName().toString();
        return normalizedPath.isBlank()
            ? UUID.randomUUID() + "_" + sanitizedFileName
            : normalizedPath + "/" + UUID.randomUUID() + "_" + sanitizedFileName;
    }

    private String extractPathFromKey(String key) {
        String normalizedKey = normalizeRelativePath(key);
        int separatorIndex = normalizedKey.lastIndexOf('/');
        return separatorIndex < 0 ? "" : normalizedKey.substring(0, separatorIndex);
    }

    private String normalizeRelativePath(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.replace('\\', '/').replaceAll("^/+", "").replaceAll("/+", "/");
    }
}
