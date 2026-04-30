package ru.sberbank.sbercrm.saas.doctemplate.template.gateway.filestorage;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import ru.sberbank.sbercrm.saas.doctemplate.application.integration.client.FileRs;

import java.util.UUID;

public final class FileStorageWireMock {
    private static final String UPLOAD_PATH = "/internal/v1/file/upload";
    private static final String DOWNLOAD_PATH = "/internal/v1/file/download";
    private static final String DELETE_PATH = "/internal/v1/file";
    private static final String SOURCE_HEADER = "source";
    private static final String TENANT_ID_HEADER = "X-Tenant-Id";
    private static final String USER_ID_HEADER = "X-User-Id";

    private FileStorageWireMock() {
    }

    public static void stubUploadFile(
        ObjectMapper objectMapper,
        UUID tenantId,
        UUID userId,
        String source,
        String path,
        String fileName,
        String key
    ) throws JsonProcessingException {
        stubFor(
            com.github.tomakehurst.wiremock.client.WireMock.post(urlPathEqualTo(UPLOAD_PATH))
                .withHeader(SOURCE_HEADER, equalTo(source))
                .withHeader(TENANT_ID_HEADER, equalTo(tenantId.toString()))
                .withHeader(USER_ID_HEADER, equalTo(userId.toString()))
                .willReturn(
                    aResponse()
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody(
                            objectMapper.writeValueAsString(
                                FileRs.builder()
                                    .key(key)
                                    .path(path)
                                    .source(source)
                                    .fileName(fileName)
                                    .build()
                            )
                        )
                )
        );
    }

    public static void verifyUploadFile(UUID tenantId, UUID userId, String source, String path, String fileName) {
        com.github.tomakehurst.wiremock.client.WireMock.verify(
            postRequestedFor(urlPathEqualTo(UPLOAD_PATH))
                .withHeader(SOURCE_HEADER, equalTo(source))
                .withHeader(TENANT_ID_HEADER, equalTo(tenantId.toString()))
                .withHeader(USER_ID_HEADER, equalTo(userId.toString()))
                .withRequestBody(containing("name=\"property\""))
                .withRequestBody(containing("\"path\":\"" + path + "\""))
                .withRequestBody(containing("\"source\":\"" + source + "\""))
                .withRequestBody(containing("name=\"file\"; filename=\"" + fileName + "\""))
        );
    }

    public static void stubDownloadFile(UUID tenantId, UUID userId, String source, String key, byte[] content) {
        stubFor(
            com.github.tomakehurst.wiremock.client.WireMock.get(urlPathEqualTo(DOWNLOAD_PATH))
                .withHeader(SOURCE_HEADER, equalTo(source))
                .withHeader(TENANT_ID_HEADER, equalTo(tenantId.toString()))
                .withHeader(USER_ID_HEADER, equalTo(userId.toString()))
                .withQueryParam("key", equalTo(key))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/force-download")
                        .withBody(content)
                )
        );
    }

    public static void verifyDownloadFile(UUID tenantId, UUID userId, String source, String key) {
        com.github.tomakehurst.wiremock.client.WireMock.verify(
            getRequestedFor(urlPathEqualTo(DOWNLOAD_PATH))
                .withHeader(SOURCE_HEADER, equalTo(source))
                .withHeader(TENANT_ID_HEADER, equalTo(tenantId.toString()))
                .withHeader(USER_ID_HEADER, equalTo(userId.toString()))
                .withQueryParam("key", equalTo(key))
        );
    }

    public static void stubDeleteFile(UUID tenantId, UUID userId, String source, String key) {
        stubFor(
            com.github.tomakehurst.wiremock.client.WireMock.delete(urlPathEqualTo(DELETE_PATH))
                .withHeader(SOURCE_HEADER, equalTo(source))
                .withHeader(TENANT_ID_HEADER, equalTo(tenantId.toString()))
                .withHeader(USER_ID_HEADER, equalTo(userId.toString()))
                .withQueryParam("key", equalTo(key))
                .willReturn(aResponse().withStatus(200))
        );
    }

    public static void verifyDeleteFile(UUID tenantId, UUID userId, String source, String key) {
        com.github.tomakehurst.wiremock.client.WireMock.verify(
            deleteRequestedFor(urlPathEqualTo(DELETE_PATH))
                .withHeader(SOURCE_HEADER, equalTo(source))
                .withHeader(TENANT_ID_HEADER, equalTo(tenantId.toString()))
                .withHeader(USER_ID_HEADER, equalTo(userId.toString()))
                .withQueryParam("key", equalTo(key))
        );
    }
}
