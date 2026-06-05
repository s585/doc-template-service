package ru.sberbank.sbercrm.saas.doctemplate.application.integration.gateway;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.attribute.FileTime;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import ru.sberbank.sbercrm.saas.doctemplate.application.exception.CrmErrorCodes;
import ru.sberbank.sbercrm.saas.doctemplate.application.exception.model.SystemCrmException;
import ru.sberbank.sbercrm.saas.doctemplate.application.integration.client.FileFilterRq;
import ru.sberbank.sbercrm.saas.doctemplate.application.integration.client.FileRs;
import ru.sberbank.sbercrm.saas.doctemplate.template.properties.DocTemplateProperties;
import ru.sberbank.sbercrm.saas.doctemplate.template.util.TemplateFileUtils;

@Component
@ConditionalOnProperty(prefix = "saas.doc-template.file-storage", name = "stub-enabled", havingValue = "true")
@RequiredArgsConstructor
public class FileStorageGatewayStub implements FileStorageGateway {
    private final DocTemplateProperties docTemplateProperties;

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
            throw new SystemCrmException(CrmErrorCodes.FILE_STORAGE_REQUEST_FAILED, CrmErrorCodes.FILE_STORAGE_REQUEST_FAILED, ex, path);
        }
    }

    @Override
    public byte[] download(UUID tenantId, UUID userId, String key) {
        try {
            Path targetPath = resolveStoragePath(key);
            if (!Files.exists(targetPath) || !Files.isRegularFile(targetPath)) {
                throw new IOException("File not found by key: " + key);
            }
            return Files.readAllBytes(targetPath);
        } catch (IOException ex) {
            throw new SystemCrmException(CrmErrorCodes.FILE_STORAGE_REQUEST_FAILED, CrmErrorCodes.FILE_STORAGE_REQUEST_FAILED, ex, key);
        }
    }

    @Override
    public List<FileRs> findAllByFilter(UUID tenantId, UUID userId, FileFilterRq filter) {
        Path rootPath = resolveStorageRootPath();
        if (!Files.exists(rootPath)) {
            return List.of();
        }
        try (Stream<Path> stream = Files.walk(rootPath)) {
            return stream
                .filter(Files::isRegularFile)
                .map(this::buildStoredFileResponse)
                .filter(file -> matchesFilter(file, filter))
                .sorted(
                    Comparator.comparing(FileRs::getUpdatedDate, Comparator.nullsLast(Comparator.naturalOrder()))
                        .reversed()
                )
                .toList();
        } catch (IOException ex) {
            throw new SystemCrmException(
                CrmErrorCodes.FILE_STORAGE_REQUEST_FAILED,
                CrmErrorCodes.FILE_STORAGE_REQUEST_FAILED,
                ex,
                filter.getPrefixKey()
            );
        }
    }

    @Override
    public void deleteFile(UUID tenantId, UUID userId, String key) {
        try {
            Files.deleteIfExists(resolveStoragePath(key));
        } catch (IOException ex) {
            throw new SystemCrmException(CrmErrorCodes.FILE_STORAGE_REQUEST_FAILED, CrmErrorCodes.FILE_STORAGE_REQUEST_FAILED, ex, key);
        }
    }

    private FileRs buildResponse(String key, String path, Path targetPath, String fileName) {
        OffsetDateTime updatedDate = resolveUpdatedDate(targetPath);
        return FileRs.builder()
            .key(key)
            .path(targetPath.toAbsolutePath().toString())
            .source(docTemplateProperties.getFileStorage().getSource())
            .fileName(fileName)
            .size(resolveFileSize(targetPath))
            .createdDate(updatedDate)
            .updatedDate(updatedDate)
            .build();
    }

    private FileRs buildStoredFileResponse(Path targetPath) {
        String key = normalizeRelativePath(
            resolveStorageRootPath().relativize(targetPath).toString()
        );
        OffsetDateTime updatedDate = resolveUpdatedDate(targetPath);
        return FileRs.builder()
            .key(key)
            .path(targetPath.toAbsolutePath().toString())
            .source(docTemplateProperties.getFileStorage().getSource())
            .fileName(resolveOriginalFileName(targetPath.getFileName().toString()))
            .size(resolveFileSize(targetPath))
            .createdDate(updatedDate)
            .updatedDate(updatedDate)
            .build();
    }

    private Path resolveStoragePath(String key) {
        Path rootPath = resolveStorageRootPath();
        Path targetPath = rootPath
            .resolve(normalizeRelativePath(key))
            .normalize();
        if (!targetPath.startsWith(rootPath)) {
            throw new SystemCrmException(
                CrmErrorCodes.FILE_STORAGE_REQUEST_FAILED,
                CrmErrorCodes.FILE_STORAGE_REQUEST_FAILED,
                key
            );
        }
        return targetPath;
    }

    private Path resolveStorageRootPath() {
        return Path.of(docTemplateProperties.getFileStorage().getStubRootPath())
            .toAbsolutePath()
            .normalize();
    }

    private String buildKey(String path, String fileName) {
        String normalizedPath = normalizeRelativePath(path);
        String sanitizedFileName = Path.of(fileName).getFileName().toString();
        String randomPrefix = UUID.randomUUID().toString();
        return normalizedPath.isBlank()
            ? randomPrefix + "_" + sanitizedFileName
            : normalizedPath + "/" + randomPrefix + "_" + sanitizedFileName;
    }

    private boolean matchesFilter(FileRs file, FileFilterRq filter) {
        if (filter == null) {
            return true;
        }
        return matchesPrefix(file.getKey(), filter.getPrefixKey())
            && matchesOriginalFileName(file.getFileName(), filter.getOriginalFileName())
            && matchesSource(file.getSource(), filter.getSource());
    }

    private boolean matchesPrefix(String key, String prefixKey) {
        return prefixKey == null || prefixKey.isBlank() || key.startsWith(normalizeRelativePath(prefixKey));
    }

    private boolean matchesOriginalFileName(String fileName, String originalFileName) {
        return originalFileName == null || originalFileName.isBlank() || originalFileName.equals(fileName);
    }

    private boolean matchesSource(String source, String expectedSource) {
        return expectedSource == null || expectedSource.isBlank() || expectedSource.equals(source);
    }

    private long resolveFileSize(Path targetPath) {
        try {
            return Files.size(targetPath);
        } catch (IOException ex) {
            throw new SystemCrmException(
                CrmErrorCodes.FILE_STORAGE_REQUEST_FAILED,
                CrmErrorCodes.FILE_STORAGE_REQUEST_FAILED,
                ex,
                targetPath.toString()
            );
        }
    }

    private OffsetDateTime resolveUpdatedDate(Path targetPath) {
        try {
            FileTime lastModifiedTime = Files.getLastModifiedTime(targetPath);
            return OffsetDateTime.ofInstant(lastModifiedTime.toInstant(), ZoneOffset.UTC);
        } catch (IOException ex) {
            throw new SystemCrmException(
                CrmErrorCodes.FILE_STORAGE_REQUEST_FAILED,
                CrmErrorCodes.FILE_STORAGE_REQUEST_FAILED,
                ex,
                targetPath.toString()
            );
        }
    }

    private String resolveOriginalFileName(String storedFileName) {
        int delimiterIndex = storedFileName.indexOf('_');
        if (delimiterIndex <= 0) {
            return storedFileName;
        }
        String prefix = storedFileName.substring(0, delimiterIndex);
        return isUuid(prefix) ? storedFileName.substring(delimiterIndex + 1) : storedFileName;
    }

    private boolean isUuid(String value) {
        try {
            UUID.fromString(value);
            return true;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private String normalizeRelativePath(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.replace('\\', '/').replaceAll("^/+", "").replaceAll("/+", "/");
    }
}
