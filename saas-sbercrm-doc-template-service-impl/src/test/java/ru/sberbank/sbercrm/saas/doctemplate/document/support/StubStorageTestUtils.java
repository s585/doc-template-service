package ru.sberbank.sbercrm.saas.doctemplate.document.support;

import java.nio.file.Files;
import java.nio.file.Path;
import ru.sberbank.sbercrm.saas.doctemplate.template.properties.FileStorageProperties;

public final class StubStorageTestUtils {
    private StubStorageTestUtils() {
    }

    public static void writeToStubStorage(
        FileStorageProperties fileStorageProperties,
        String key,
        byte[] content
    ) throws Exception {
        Path filePath = Path.of(fileStorageProperties.getLocal().getRootPath()).resolve(key);
        Files.createDirectories(filePath.getParent());
        Files.write(filePath, content);
    }

    public static void deleteFromStubStorage(
        FileStorageProperties fileStorageProperties,
        String key
    ) throws Exception {
        Path filePath = Path.of(fileStorageProperties.getLocal().getRootPath()).resolve(key);
        Files.deleteIfExists(filePath);
    }
}
