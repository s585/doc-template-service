package ru.sberbank.sbercrm.saas.doctemplate.application.integration.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import ru.sberbank.sbercrm.saas.doctemplate.application.integration.client.FileRs;
import ru.sberbank.sbercrm.saas.doctemplate.template.properties.TemplateProperties;

class FileStorageGatewayStubTest {
    private static final UUID TENANT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("Stub сохраняет файл локально и позволяет скачать его по key")
    void givenUploadedFile_whenDownload_thenReturnStoredFileInfo() throws Exception {
        // given
        TemplateProperties properties = new TemplateProperties();
        properties.getFileStorage().setSource("doc-template-service");
        properties.getFileStorage().setStubRootPath(tempDir.toString());
        FileStorageGatewayStub systemUnderTest = new FileStorageGatewayStub(properties);
        MockMultipartFile file = new MockMultipartFile("file", "template.docx", null, "hello".getBytes());

        // when
        FileRs uploaded = systemUnderTest.upload(TENANT_ID, USER_ID, "/doc-template/123", "test", file);
        FileRs downloaded = systemUnderTest.download(TENANT_ID, USER_ID, uploaded.getKey());

        // then
        assertThat(downloaded.getKey()).isEqualTo(uploaded.getKey());
        assertThat(downloaded.getFileName()).endsWith("template.docx");
        assertThat(Files.readString(Path.of(downloaded.getPath()))).isEqualTo("hello");
    }
}
