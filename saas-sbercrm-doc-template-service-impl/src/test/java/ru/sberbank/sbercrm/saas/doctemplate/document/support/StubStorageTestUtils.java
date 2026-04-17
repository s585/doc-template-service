package ru.sberbank.sbercrm.saas.doctemplate.document.support;

import java.nio.file.Files;
import java.nio.file.Path;
import ru.sberbank.sbercrm.saas.doctemplate.template.properties.DocTemplateProperties;

public final class StubStorageTestUtils {
    private StubStorageTestUtils() {
    }

    public static void writeToStubStorage(
        DocTemplateProperties docTemplateProperties,
        String key,
        byte[] content
    ) throws Exception {
        Path filePath = Path.of(docTemplateProperties.getFileStorage().getStubRootPath()).resolve(key);
        Files.createDirectories(filePath.getParent());
        Files.write(filePath, content);
    }

    public static void deleteFromStubStorage(
        DocTemplateProperties docTemplateProperties,
        String key
    ) throws Exception {
        Path filePath = Path.of(docTemplateProperties.getFileStorage().getStubRootPath()).resolve(key);
        Files.deleteIfExists(filePath);
    }
}
