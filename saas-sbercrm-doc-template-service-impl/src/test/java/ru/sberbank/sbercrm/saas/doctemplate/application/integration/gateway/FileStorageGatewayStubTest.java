package ru.sberbank.sbercrm.saas.doctemplate.application.integration.gateway;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import ru.sberbank.sbercrm.saas.doctemplate.application.exception.model.SystemCrmException;
import ru.sberbank.sbercrm.saas.doctemplate.application.integration.client.FileFilterRq;
import ru.sberbank.sbercrm.saas.doctemplate.application.integration.client.FileRs;
import ru.sberbank.sbercrm.saas.doctemplate.template.properties.DocTemplateProperties;

class FileStorageGatewayStubTest {
    private static final UUID TENANT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("Заглушка сохраняет файл локально и позволяет скачать его по ключу")
    void givenUploadedFile_whenDownload_thenReturnStoredFileInfo() throws Exception {
        // given
        DocTemplateProperties properties = new DocTemplateProperties();
        properties.getFileStorage().setSource("doc-template-service");
        properties.getFileStorage().setStubRootPath(tempDir.toString());
        FileStorageGatewayStub systemUnderTest = new FileStorageGatewayStub(properties);
        MockMultipartFile file = new MockMultipartFile("file", "template.docx", null, "hello".getBytes());

        // when
        FileRs uploaded = systemUnderTest.upload(TENANT_ID, USER_ID, "templates/123", "test", file);
        byte[] downloaded = systemUnderTest.download(TENANT_ID, USER_ID, uploaded.getKey());

        // then
        assertThat(new String(downloaded)).isEqualTo("hello");
    }

    @Test
    @DisplayName("Заглушка сохраняет generated файл как отдельные локальные файлы")
    void givenGeneratedFileUpload_whenUploadTwice_thenCreateDifferentKeys() throws Exception {
        DocTemplateProperties properties = new DocTemplateProperties();
        properties.getFileStorage().setSource("doc-template-service");
        properties.getFileStorage().setStubRootPath(tempDir.toString());
        FileStorageGatewayStub systemUnderTest = new FileStorageGatewayStub(properties);
        MockMultipartFile firstFile =
            new MockMultipartFile("file", "result.docx", null, "first".getBytes());
        MockMultipartFile secondFile =
            new MockMultipartFile("file", "result.docx", null, "second".getBytes());

        FileRs firstUpload =
            systemUnderTest.upload(TENANT_ID, USER_ID, "documents/entity/object/document", "test", firstFile);
        FileRs secondUpload =
            systemUnderTest.upload(TENANT_ID, USER_ID, "documents/entity/object/document", "test", secondFile);

        assertThat(firstUpload.getKey()).isNotEqualTo(secondUpload.getKey());
        assertThat(systemUnderTest.download(TENANT_ID, USER_ID, firstUpload.getKey()))
            .isEqualTo("first".getBytes());
        assertThat(systemUnderTest.download(TENANT_ID, USER_ID, secondUpload.getKey()))
            .isEqualTo("second".getBytes());
    }

    @Test
    @DisplayName("Заглушка умеет находить файлы по prefixKey и originalFileName")
    void givenStoredFiles_whenGetWithFilter_thenReturnMatchingFiles() {
        DocTemplateProperties properties = new DocTemplateProperties();
        properties.getFileStorage().setSource("doc-template-service");
        properties.getFileStorage().setStubRootPath(tempDir.toString());
        FileStorageGatewayStub systemUnderTest = new FileStorageGatewayStub(properties);
        MockMultipartFile matchingFile =
            new MockMultipartFile("file", "result.docx", null, "first".getBytes());
        MockMultipartFile otherFile =
            new MockMultipartFile("file", "other.docx", null, "second".getBytes());

        FileRs storedMatchingFile = systemUnderTest.upload(
            TENANT_ID,
            USER_ID,
            "documents/entity/object/document",
            "test",
            matchingFile
        );
        systemUnderTest.upload(
            TENANT_ID,
            USER_ID,
            "documents/entity/object/another-document",
            "test",
            otherFile
        );

        List<FileRs> foundFiles = systemUnderTest.findAllByFilter(
            TENANT_ID,
            USER_ID,
            FileFilterRq.builder()
                .source("doc-template-service")
                .prefixKey("documents/entity/object/document")
                .originalFileName("result.docx")
                .build()
        );

        assertThat(foundFiles).singleElement().satisfies(file -> {
            assertThat(file.getKey()).isEqualTo(storedMatchingFile.getKey());
            assertThat(file.getFileName()).isEqualTo("result.docx");
            assertThat(file.getSize()).isEqualTo(5L);
        });
    }

    @Test
    @DisplayName("Заглушка не позволяет прочитать файл за пределами локального корня")
    void givenEscapingKey_whenDownload_thenThrowException() {
        DocTemplateProperties properties = new DocTemplateProperties();
        properties.getFileStorage().setSource("doc-template-service");
        properties.getFileStorage().setStubRootPath(tempDir.toString());
        FileStorageGatewayStub systemUnderTest = new FileStorageGatewayStub(properties);

        assertThatThrownBy(() -> systemUnderTest.download(TENANT_ID, USER_ID, "../secret.docx"))
            .isInstanceOf(SystemCrmException.class);
    }
}
