package ru.sberbank.sbercrm.saas.doctemplate.application.integration.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import ru.sberbank.sbercrm.saas.doctemplate.AbstractIntegrationTest;

@TestPropertySource(properties = "saas.doc-template.file-storage.stub-enabled=false")
class FeignFileStorageGatewayIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private FileStorageGateway fileStorageGateway;

    @Test
    @DisplayName("Feign gateway скачивает бинарный файл через реальный HTTP ответ file storage")
    void givenBinaryDownloadResponse_whenDownload_thenReturnBytes() {
        String fileStorageSource = "doc-template-service";
        String fileKey = "templates/test-download.docx";
        String normalizedKey = "/templates/test-download.docx";
        byte[] expectedContent = "binary-template-content".getBytes(StandardCharsets.UTF_8);
        fileStorageWireMock.stubDownloadFile(TENANT_ID, USER_ID, fileStorageSource, normalizedKey, expectedContent);

        byte[] downloaded = fileStorageGateway.download(TENANT_ID, USER_ID, fileKey);

        assertThat(downloaded).isEqualTo(expectedContent);
        fileStorageWireMock.verifyDownloadFile(TENANT_ID, USER_ID, fileStorageSource, normalizedKey);
    }
}
