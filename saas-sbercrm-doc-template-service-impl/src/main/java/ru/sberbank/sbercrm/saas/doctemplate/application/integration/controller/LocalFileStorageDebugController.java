package ru.sberbank.sbercrm.saas.doctemplate.application.integration.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.sberbank.sbercrm.saas.doctemplate.application.exception.CrmErrorCodes;
import ru.sberbank.sbercrm.saas.doctemplate.application.exception.model.SystemCrmException;
import ru.sberbank.sbercrm.saas.doctemplate.application.integration.dto.LocalFileStorageFileRs;
import ru.sberbank.sbercrm.saas.doctemplate.template.properties.FileStorageProperties;

@RestController
@RequestMapping("/internal/dev/file-storage")
@ConditionalOnProperty(prefix = "saas.doc-template.file-storage.local", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class LocalFileStorageDebugController {
    private final FileStorageProperties fileStorageProperties;

    @GetMapping("/file")
    public LocalFileStorageFileRs getFile(@RequestParam("key") String key) {
        Path rootPath = resolveStorageRootPath();
        Path filePath = resolveStoragePath(rootPath, key);
        boolean exists = Files.exists(filePath);
        boolean regularFile = Files.isRegularFile(filePath);

        return LocalFileStorageFileRs.builder()
            .key(normalizeRelativePath(key))
            .rootPath(rootPath.toString())
            .path(filePath.toString())
            .exists(exists)
            .regularFile(regularFile)
            .sizeBytes(regularFile ? resolveFileSize(filePath, key) : null)
            .build();
    }

    private Path resolveStorageRootPath() {
        return Path.of(fileStorageProperties.getLocal().getRootPath())
            .toAbsolutePath()
            .normalize();
    }

    private Path resolveStoragePath(Path rootPath, String key) {
        Path filePath = rootPath.resolve(normalizeRelativePath(key)).normalize();
        if (!filePath.startsWith(rootPath)) {
            throw new SystemCrmException(
                CrmErrorCodes.FILE_STORAGE_REQUEST_FAILED,
                CrmErrorCodes.FILE_STORAGE_REQUEST_FAILED,
                key
            );
        }
        return filePath;
    }

    private Long resolveFileSize(Path filePath, String key) {
        try {
            return Files.size(filePath);
        } catch (IOException ex) {
            throw new SystemCrmException(
                CrmErrorCodes.FILE_STORAGE_REQUEST_FAILED,
                CrmErrorCodes.FILE_STORAGE_REQUEST_FAILED,
                ex,
                key
            );
        }
    }

    private String normalizeRelativePath(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.replace('\\', '/').replaceAll("^/+", "").replaceAll("/+", "/");
    }
}
