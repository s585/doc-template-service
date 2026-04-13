package ru.sberbank.sbercrm.saas.doctemplate.template.gateway.filestorage;

import static org.mockito.BDDMockito.willThrow;
import static org.mockito.BDDMockito.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import feign.FeignException;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import ru.sberbank.sbercrm.saas.doctemplate.application.integration.client.FileStorageClient;
import ru.sberbank.sbercrm.saas.doctemplate.application.integration.client.FileFilterRq;
import ru.sberbank.sbercrm.saas.doctemplate.application.integration.client.FileRs;
import ru.sberbank.sbercrm.saas.doctemplate.application.integration.gateway.FeignFileStorageGateway;
import ru.sberbank.sbercrm.saas.doctemplate.template.properties.DocTemplateProperties;

@ExtendWith(MockitoExtension.class)
class FeignFileStorageGatewayTest {
    private static final UUID TENANT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Mock
    private FileStorageClient fileStorageClient;

    @Mock
    private DocTemplateProperties docTemplateProperties;

    @InjectMocks
    private FeignFileStorageGateway systemUnderTest;

    @BeforeEach
    void setUp() {
        DocTemplateProperties.FileStorage fileStorage = new DocTemplateProperties.FileStorage();
        fileStorage.setSource("doc-template-service");
        org.mockito.BDDMockito.given(docTemplateProperties.getFileStorage()).willReturn(fileStorage);
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
        systemUnderTest.deleteFile(TENANT_ID, USER_ID, fileKey);

        verify(fileStorageClient).deleteFile("doc-template-service", fileKey, TENANT_ID, USER_ID);
        verifyNoMoreInteractions(fileStorageClient);
    }

    @Test
    @DisplayName("Скачивание файла вычитывает response body в byte array")
    void givenDownloadResponse_whenDownload_thenReturnBytes() {
        // given
        String fileKey = "templates/test.docx";
        ResponseEntity<InputStreamResource> response = new ResponseEntity<>(
            new InputStreamResource(new ByteArrayInputStream("hello".getBytes(StandardCharsets.UTF_8))),
            HttpStatus.OK
        );
        given(fileStorageClient.download("doc-template-service", fileKey, TENANT_ID, USER_ID)).willReturn(response);

        // when
        byte[] downloaded = systemUnderTest.download(TENANT_ID, USER_ID, fileKey);

        // then
        assertThat(downloaded).isEqualTo("hello".getBytes(StandardCharsets.UTF_8));
        verify(fileStorageClient).download("doc-template-service", fileKey, TENANT_ID, USER_ID);
    }

    @Test
    @DisplayName("Поиск файлов по фильтру проксируется в file storage client")
    void givenFilter_whenFindAllByFilter_thenReturnClientResponse() {
        FileFilterRq filter = FileFilterRq.builder()
            .prefixKey("doc-template/generated/document")
            .originalFileName("result.docx")
            .build();
        List<FileRs> expectedFiles = List.of(
            FileRs.builder().key("generated/result.docx").fileName("result.docx").build()
        );
        given(fileStorageClient.getWithFilter("doc-template-service", filter, TENANT_ID, USER_ID))
            .willReturn(expectedFiles);

        List<FileRs> files = systemUnderTest.findAllByFilter(TENANT_ID, USER_ID, filter);

        assertThat(files).isEqualTo(expectedFiles);
        verify(fileStorageClient).getWithFilter("doc-template-service", filter, TENANT_ID, USER_ID);
    }
}
