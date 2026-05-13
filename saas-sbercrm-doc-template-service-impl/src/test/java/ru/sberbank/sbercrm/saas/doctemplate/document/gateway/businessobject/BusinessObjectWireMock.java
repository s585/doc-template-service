package ru.sberbank.sbercrm.saas.doctemplate.document.gateway.businessobject;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.http.MediaType;
import ru.sberbank.sbercrm.saas.doctemplate.shared.dto.CommonRqDto;
import ru.sberbank.sbercrm.saas.doctemplate.shared.dto.SelectDto;

@TestComponent
@RequiredArgsConstructor
public class BusinessObjectWireMock {
    private static final String TENANT_ID_HEADER = "X-Tenant-Id";
    private static final String USER_ID_HEADER = "X-User-Id";

    private final ObjectMapper objectMapper;

    public void stubGetObjectWithSpecifiedFields(
        UUID tenantId,
        UUID userId,
        UUID entityId,
        UUID objectId,
        SelectDto selectDto,
        Map<String, Object> body
    ) throws JsonProcessingException {
        String path = buildObjectPath(entityId, objectId);
        stubFor(
            com.github.tomakehurst.wiremock.client.WireMock.post(urlPathEqualTo(path))
                .withHeader(TENANT_ID_HEADER, equalTo(tenantId.toString()))
                .withHeader(USER_ID_HEADER, equalTo(userId.toString()))
                .withRequestBody(equalToJson(objectMapper.writeValueAsString(selectDto), true, true))
                .willReturn(
                    aResponse()
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody(objectMapper.writeValueAsString(body))
                )
        );
    }

    public void verifyGetObjectWithSpecifiedFields(
        UUID tenantId,
        UUID userId,
        UUID entityId,
        UUID objectId,
        SelectDto selectDto
    ) throws JsonProcessingException {
        String path = buildObjectPath(entityId, objectId);
        verify(
            postRequestedFor(urlPathEqualTo(path))
                .withHeader(TENANT_ID_HEADER, equalTo(tenantId.toString()))
                .withHeader(USER_ID_HEADER, equalTo(userId.toString()))
                .withRequestBody(equalToJson(objectMapper.writeValueAsString(selectDto), true, true))
        );
    }

    public void stubListObjects(
        UUID tenantId,
        UUID userId,
        UUID entityId,
        CommonRqDto request,
        Map<String, Object> body
    ) throws JsonProcessingException {
        String path = "/internal/data/" + entityId + "/list-objects";
        stubFor(
            com.github.tomakehurst.wiremock.client.WireMock.post(urlPathEqualTo(path))
                .withHeader(TENANT_ID_HEADER, equalTo(tenantId.toString()))
                .withHeader(USER_ID_HEADER, equalTo(userId.toString()))
                .withRequestBody(equalToJson(objectMapper.writeValueAsString(request), true, true))
                .willReturn(
                    aResponse()
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody(objectMapper.writeValueAsString(body))
                )
        );
    }

    private String buildObjectPath(UUID entityId, UUID objectId) {
        return "/internal/data/" + entityId + "/" + objectId;
    }
}
