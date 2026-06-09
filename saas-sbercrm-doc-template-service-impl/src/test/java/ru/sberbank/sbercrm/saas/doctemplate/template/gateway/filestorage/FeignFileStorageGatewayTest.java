package ru.sberbank.sbercrm.saas.doctemplate.template.gateway.filestorage;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.BDDMockito.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import feign.FeignException;
import feign.Request;
import feign.Response;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.Mockito;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import ru.sberbank.sbercrm.saas.doctemplate.application.exception.CrmErrorCodes;
import ru.sberbank.sbercrm.saas.doctemplate.application.exception.model.SystemCrmException;
import ru.sberbank.sbercrm.saas.doctemplate.application.integration.client.FileStorageClient;
import ru.sberbank.sbercrm.saas.doctemplate.application.integration.client.FileFilterRq;
import ru.sberbank.sbercrm.saas.doctemplate.application.integration.client.FileRq;
import ru.sberbank.sbercrm.saas.doctemplate.application.integration.client.FileRs;
import ru.sberbank.sbercrm.saas.doctemplate.application.integration.gateway.FeignFileStorageGateway;
import ru.sberbank.sbercrm.saas.doctemplate.template.properties.FileStorageProperties;

@ExtendWith(MockitoExtension.class)
class FeignFileStorageGatewayTest {
    private static final UUID TENANT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Mock
    private FileStorageClient fileStorageClient;

    private final FileStorageProperties fileStorageProperties = new FileStorageProperties();

    private FeignFileStorageGateway systemUnderTest;

    @BeforeEach
    void setUp() {
        fileStorageProperties.setNamespace("doc-template-service");
        systemUnderTest = new FeignFileStorageGateway(fileStorageClient, fileStorageProperties);
    }

    @Test
    @DisplayName("Загрузка файла проксируется в file storage client")
    void givenUploadRequest_whenUpload_thenReturnClientResponse() throws Exception {
        String path = "/doc-template/templates";
        String description = "contract template";
        MultipartFile file = new MockMultipartFile("file", "template.docx", "application/octet-stream", "hello".getBytes(StandardCharsets.UTF_8));
        FileRs expected = FileRs.builder().key("templates/template.docx").path(path).build();
        given(fileStorageClient.upload(
            "doc-template-service",
            FileRq.builder()
                .path(path)
                .source("doc-template-service")
                .description(description)
                .build(),
            file,
            TENANT_ID,
            USER_ID
        )).willReturn(expected);

        FileRs uploaded = systemUnderTest.upload(TENANT_ID, USER_ID, path, description, file);

        assertThat(uploaded).isEqualTo(expected);
    }

    @Test
    @DisplayName("Загрузка файла оборачивает ошибку клиента в SystemCrmException")
    void givenUploadFeignFailure_whenUpload_thenThrowSystemCrmException() throws Exception {
        String path = "/doc-template/templates";
        MultipartFile file = new MockMultipartFile("file", "template.docx", "application/octet-stream", new byte[0]);
        FeignException exception = feignException(500, "upload", "http://localhost/internal/v1/file/upload");
        given(fileStorageClient.upload(
            "doc-template-service",
            FileRq.builder()
                .path(path)
                .source("doc-template-service")
                .description("desc")
                .build(),
            file,
            TENANT_ID,
            USER_ID
        )).willThrow(exception);

        assertThatThrownBy(() -> systemUnderTest.upload(TENANT_ID, USER_ID, path, "desc", file))
            .isInstanceOf(SystemCrmException.class)
            .hasFieldOrPropertyWithValue("code", CrmErrorCodes.FILE_STORAGE_REQUEST_FAILED);
    }

    @Test
    @DisplayName("Удаление файла игнорирует отсутствие файла в file storage")
    void givenMissingFile_whenDeleteFile_thenIgnoreNotFound() {
        // given
        String fileKey = "templates/test.docx";
        String normalizedKey = "/templates/test.docx";
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
        ).given(fileStorageClient).deleteFile("doc-template-service", normalizedKey, TENANT_ID, USER_ID);

        // expected
        systemUnderTest.deleteFile(TENANT_ID, USER_ID, fileKey);

        verify(fileStorageClient).deleteFile("doc-template-service", normalizedKey, TENANT_ID, USER_ID);
        verifyNoMoreInteractions(fileStorageClient);
    }

    @Test
    @DisplayName("Удаление файла нормализует ключ, начинающийся с косой черты")
    void givenSlashPrefixedKey_whenDeleteFile_thenKeepKeyUnchanged() {
        String fileKey = "/templates/test.docx";

        systemUnderTest.deleteFile(TENANT_ID, USER_ID, fileKey);

        verify(fileStorageClient).deleteFile("doc-template-service", fileKey, TENANT_ID, USER_ID);
    }

    @Test
    @DisplayName("Удаление файла оборачивает ошибку клиента в SystemCrmException")
    void givenDeleteFeignFailure_whenDeleteFile_thenThrowSystemCrmException() {
        String fileKey = "templates/test.docx";
        String normalizedKey = "/templates/test.docx";
        willThrow(feignException(500, "deleteFile", "http://localhost/internal/v1/file"))
            .given(fileStorageClient).deleteFile("doc-template-service", normalizedKey, TENANT_ID, USER_ID);

        assertThatThrownBy(() -> systemUnderTest.deleteFile(TENANT_ID, USER_ID, fileKey))
            .isInstanceOf(SystemCrmException.class)
            .hasFieldOrPropertyWithValue("code", CrmErrorCodes.FILE_STORAGE_REQUEST_FAILED);
    }

    @Test
    @DisplayName("Скачивание файла вычитывает response body в byte array")
    void givenDownloadResponse_whenDownload_thenReturnBytes() {
        // given
        String fileKey = "templates/test.docx";
        String normalizedKey = "/templates/test.docx";
        Response response = Response.builder()
            .status(200)
            .reason("OK")
            .request(
                Request.create(
                    Request.HttpMethod.GET,
                    "http://localhost/internal/v1/file/download?key=" + normalizedKey,
                    Map.of(),
                    null,
                    StandardCharsets.UTF_8,
                    null
                )
            )
            .headers(Map.of())
            .body("hello".getBytes(StandardCharsets.UTF_8))
            .build();
        given(fileStorageClient.download("doc-template-service", normalizedKey, TENANT_ID, USER_ID)).willReturn(response);

        // when
        byte[] downloaded = systemUnderTest.download(TENANT_ID, USER_ID, fileKey);

        // then
        assertThat(downloaded).isEqualTo("hello".getBytes(StandardCharsets.UTF_8));
        verify(fileStorageClient).download("doc-template-service", normalizedKey, TENANT_ID, USER_ID);
    }

    @Test
    @DisplayName("Скачивание файла не меняет ключ с ведущей косой чертой")
    void givenSlashPrefixedKey_whenDownload_thenKeepKeyUnchanged() {
        String fileKey = "/templates/test.docx";
        Response response = Response.builder()
            .status(200)
            .reason("OK")
            .request(
                Request.create(
                    Request.HttpMethod.GET,
                    "http://localhost/internal/v1/file/download?key=" + fileKey,
                    Map.of(),
                    null,
                    StandardCharsets.UTF_8,
                    null
                )
            )
            .headers(Map.of())
            .body("hello".getBytes(StandardCharsets.UTF_8))
            .build();
        given(fileStorageClient.download("doc-template-service", fileKey, TENANT_ID, USER_ID)).willReturn(response);

        byte[] downloaded = systemUnderTest.download(TENANT_ID, USER_ID, fileKey);

        assertThat(downloaded).isEqualTo("hello".getBytes(StandardCharsets.UTF_8));
        verify(fileStorageClient).download("doc-template-service", fileKey, TENANT_ID, USER_ID);
    }

    @Test
    @DisplayName("Скачивание файла оборачивает ошибку клиента в SystemCrmException")
    void givenDownloadFeignFailure_whenDownload_thenThrowSystemCrmException() {
        String fileKey = "templates/test.docx";
        String normalizedKey = "/templates/test.docx";
        given(fileStorageClient.download("doc-template-service", normalizedKey, TENANT_ID, USER_ID))
            .willThrow(feignException(500, "download", "http://localhost/internal/v1/file/download"));

        assertThatThrownBy(() -> systemUnderTest.download(TENANT_ID, USER_ID, fileKey))
            .isInstanceOf(SystemCrmException.class)
            .hasFieldOrPropertyWithValue("code", CrmErrorCodes.FILE_STORAGE_REQUEST_FAILED);
    }

    @Test
    @DisplayName("Скачивание файла падает при пустом response body")
    void givenDownloadResponseWithoutBody_whenDownload_thenThrowSystemCrmException() {
        String fileKey = "templates/test.docx";
        String normalizedKey = "/templates/test.docx";
        Response response = Response.builder()
            .status(200)
            .reason("OK")
            .request(
                Request.create(
                    Request.HttpMethod.GET,
                    "http://localhost/internal/v1/file/download?key=" + normalizedKey,
                    Map.of(),
                    null,
                    StandardCharsets.UTF_8,
                    null
                )
            )
            .headers(Map.of())
            .build();
        given(fileStorageClient.download("doc-template-service", normalizedKey, TENANT_ID, USER_ID)).willReturn(response);

        assertThatThrownBy(() -> systemUnderTest.download(TENANT_ID, USER_ID, fileKey))
            .isInstanceOf(SystemCrmException.class)
            .hasFieldOrPropertyWithValue("code", CrmErrorCodes.FILE_STORAGE_REQUEST_FAILED);
    }

    @Test
    @DisplayName("Скачивание файла оборачивает IOException в SystemCrmException")
    void givenDownloadResponseWithFailingBody_whenDownload_thenThrowSystemCrmException() {
        String fileKey = "templates/test.docx";
        String normalizedKey = "/templates/test.docx";
        Response response = Response.builder()
            .status(200)
            .reason("OK")
            .request(
                Request.create(
                    Request.HttpMethod.GET,
                    "http://localhost/internal/v1/file/download?key=" + normalizedKey,
                    Map.of(),
                    null,
                    StandardCharsets.UTF_8,
                    null
                )
            )
            .headers(Map.of())
            .body(new byte[0])
            .build();
        Response spyResponse = org.mockito.Mockito.spy(response);
        Response.Body body = Mockito.mock(Response.Body.class);
        try {
            given(body.asInputStream()).willThrow(new IOException("boom"));
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        given(spyResponse.body()).willReturn(body);
        given(fileStorageClient.download("doc-template-service", normalizedKey, TENANT_ID, USER_ID)).willReturn(spyResponse);

        assertThatThrownBy(() -> systemUnderTest.download(TENANT_ID, USER_ID, fileKey))
            .isInstanceOf(SystemCrmException.class)
            .hasFieldOrPropertyWithValue("code", CrmErrorCodes.FILE_STORAGE_REQUEST_FAILED);
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

    @Test
    @DisplayName("Поиск файлов по фильтру оборачивает ошибку клиента в SystemCrmException")
    void givenFindAllByFilterFeignFailure_whenFindAllByFilter_thenThrowSystemCrmException() {
        FileFilterRq filter = FileFilterRq.builder().prefixKey("doc-template/generated/document").build();
        given(fileStorageClient.getWithFilter("doc-template-service", filter, TENANT_ID, USER_ID))
            .willThrow(feignException(500, "getWithFilter", "http://localhost/internal/v1/file/find"));

        assertThatThrownBy(() -> systemUnderTest.findAllByFilter(TENANT_ID, USER_ID, filter))
            .isInstanceOf(SystemCrmException.class)
            .hasFieldOrPropertyWithValue("code", CrmErrorCodes.FILE_STORAGE_REQUEST_FAILED);
    }

    private FeignException feignException(int status, String methodKey, String url) {
        return FeignException.errorStatus(
            methodKey,
            feign.Response.builder()
                .status(status)
                .reason("error")
                .request(
                    feign.Request.create(
                        feign.Request.HttpMethod.GET,
                        url,
                        java.util.Map.of(),
                        new byte[0],
                        StandardCharsets.UTF_8,
                        null
                    )
                )
                .headers(java.util.Map.of())
                .build()
        );
    }
}
