package ru.sberbank.sbercrm.saas.doctemplate.document.gateway.businessobject;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.MediaType;

public final class BusinessObjectWireMock {
    private static final String GET_OBJECT_PATH = "/internal/v1/business-object";
    private static final String TENANT_ID_HEADER = "X-Tenant-Id";
    private static final String USER_ID_HEADER = "X-User-Id";

    private BusinessObjectWireMock() {
    }

    public static void stubGetObject(
        ObjectMapper objectMapper,
        UUID tenantId,
        UUID userId,
        UUID entityId,
        UUID objectId,
        Map<String, Object> body
    ) throws JsonProcessingException {
        stubFor(
            com.github.tomakehurst.wiremock.client.WireMock.get(urlPathEqualTo(GET_OBJECT_PATH))
                .withHeader(TENANT_ID_HEADER, equalTo(tenantId.toString()))
                .withHeader(USER_ID_HEADER, equalTo(userId.toString()))
                .withQueryParam("entityId", equalTo(entityId.toString()))
                .withQueryParam("objectId", equalTo(objectId.toString()))
                .willReturn(
                    aResponse()
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody(objectMapper.writeValueAsString(body))
                )
        );
    }

    public static void verifyGetObject(UUID tenantId, UUID userId, UUID entityId, UUID objectId) {
        verify(
            getRequestedFor(urlPathEqualTo(GET_OBJECT_PATH))
                .withHeader(TENANT_ID_HEADER, equalTo(tenantId.toString()))
                .withHeader(USER_ID_HEADER, equalTo(userId.toString()))
                .withQueryParam("entityId", equalTo(entityId.toString()))
                .withQueryParam("objectId", equalTo(objectId.toString()))
        );
    }
}
