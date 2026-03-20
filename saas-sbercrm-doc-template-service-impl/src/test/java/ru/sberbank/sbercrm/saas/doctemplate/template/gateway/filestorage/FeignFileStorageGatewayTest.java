package ru.sberbank.sbercrm.saas.doctemplate.template.gateway.filestorage;

import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import feign.FeignException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.sberbank.sbercrm.saas.doctemplate.template.gateway.filestorage.FeignFileStorageGateway;
import ru.sberbank.sbercrm.saas.doctemplate.template.gateway.filestorage.FileStorageClient;
import ru.sberbank.sbercrm.saas.doctemplate.template.properties.TemplateProperties;

@ExtendWith(MockitoExtension.class)
class FeignFileStorageGatewayTest {
    private static final UUID TENANT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Mock
    private FileStorageClient fileStorageClient;

    @Mock
    private TemplateProperties templateProperties;

    @InjectMocks
    private FeignFileStorageGateway fileStorageGateway;

    @BeforeEach
    void setUp() {
        TemplateProperties.FileStorage fileStorage = new TemplateProperties.FileStorage();
        fileStorage.setSource("doc-template-service");
        org.mockito.BDDMockito.given(templateProperties.getFileStorage()).willReturn(fileStorage);
    }

    @Test
    @DisplayName("Удаление файла игнорирует отсутствие файла в file storage")
    void givenMissingFile_whenDeleteFile_thenIgnoreNotFound() {
        // given
        String fileKey = "templates/test.docx";
        willThrow(
            FeignException.errorStatus(
                "deleteFile",
                feign.Response.builder()
                    .status(404)
                    .reason("Not Found")
                    .request(
                        feign.Request.create(
                            feign.Request.HttpMethod.DELETE,
                            "http://localhost/internal/v1/file",
                            java.util.Map.of(),
                            new byte[0],
                            StandardCharsets.UTF_8,
                            null
                        )
                    )
                    .headers(java.util.Map.of())
                    .build()
            )
        ).given(fileStorageClient).deleteFile("doc-template-service", fileKey, TENANT_ID, USER_ID);

        // expected
        fileStorageGateway.deleteFile(TENANT_ID, USER_ID, fileKey);

        verify(fileStorageClient).deleteFile("doc-template-service", fileKey, TENANT_ID, USER_ID);
        verifyNoMoreInteractions(fileStorageClient);
    }
}
