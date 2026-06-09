package ru.sberbank.sbercrm.saas.doctemplate.application.integration.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.sberbank.sbercrm.saas.doctemplate.application.exception.model.SystemCrmException;
import ru.sberbank.sbercrm.saas.doctemplate.application.integration.dto.LocalFileStorageFileRs;
import ru.sberbank.sbercrm.saas.doctemplate.template.properties.FileStorageProperties;

class LocalFileStorageDebugControllerTest {
    @TempDir
    Path tempDir;

    @Test
    @DisplayName("Dev controller возвращает локальный путь файла по storage key")
    void givenExistingFile_whenGetFile_thenReturnLocalFileInfo() throws Exception {
        FileStorageProperties properties = buildFileStorageProperties();
        Path storedFile = tempDir.resolve("documents/result.docx");
        Files.createDirectories(storedFile.getParent());
        Files.writeString(storedFile, "hello");
        LocalFileStorageDebugController systemUnderTest = new LocalFileStorageDebugController(properties);

        LocalFileStorageFileRs response = systemUnderTest.getFile("/documents/result.docx");

        assertThat(response.getKey()).isEqualTo("documents/result.docx");
        assertThat(response.getRootPath()).isEqualTo(tempDir.toAbsolutePath().normalize().toString());
        assertThat(response.getPath()).isEqualTo(storedFile.toAbsolutePath().normalize().toString());
        assertThat(response.isExists()).isTrue();
        assertThat(response.isRegularFile()).isTrue();
        assertThat(response.getSizeBytes()).isEqualTo(5L);
    }

    @Test
    @DisplayName("Dev controller не позволяет выйти за пределы локального корня")
    void givenEscapingKey_whenGetFile_thenThrowException() {
        FileStorageProperties properties = buildFileStorageProperties();
        LocalFileStorageDebugController systemUnderTest = new LocalFileStorageDebugController(properties);

        assertThatThrownBy(() -> systemUnderTest.getFile("../secret.docx"))
            .isInstanceOf(SystemCrmException.class);
    }

    private FileStorageProperties buildFileStorageProperties() {
        FileStorageProperties properties = new FileStorageProperties();
        properties.getLocal().setRootPath(tempDir.toString());
        return properties;
    }
}
