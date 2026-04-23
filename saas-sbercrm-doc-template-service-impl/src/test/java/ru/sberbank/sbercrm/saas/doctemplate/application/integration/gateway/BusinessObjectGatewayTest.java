package ru.sberbank.sbercrm.saas.doctemplate.application.integration.gateway;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;

import feign.FeignException;
import feign.RetryableException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.net.SocketTimeoutException;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.sberbank.sbercrm.saas.doctemplate.application.exception.CrmErrorCodes;
import ru.sberbank.sbercrm.saas.doctemplate.application.exception.model.NotFoundCrmException;
import ru.sberbank.sbercrm.saas.doctemplate.application.exception.model.SystemCrmException;
import ru.sberbank.sbercrm.saas.doctemplate.application.integration.client.CoreDataClient;
import ru.sberbank.sbercrm.saas.doctemplate.document.constant.DocumentConstants;

@ExtendWith(MockitoExtension.class)
class BusinessObjectGatewayTest {
    private static final UUID TENANT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID ENTITY_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID OBJECT_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");

    @Mock
    private CoreDataClient coreDataClient;

    @InjectMocks
    private BusinessObjectGateway systemUnderTest;

    @Test
    @DisplayName("Gateway проксирует успешное получение объекта в feign client")
    void givenObjectExists_whenGetObject_thenReturnBody() throws Exception {
        Map<String, Object> expected = Map.of("customer", Map.of("name", "BO LLC"));
        given(coreDataClient.getObject(TENANT_ID, USER_ID, OBJECT_ID, ENTITY_ID))
            .willReturn(expected);

        Map<String, Object> actual = systemUnderTest.getObject(TENANT_ID, USER_ID, ENTITY_ID, OBJECT_ID);

        assertThat(actual).isEqualTo(expected);
        verify(coreDataClient).getObject(TENANT_ID, USER_ID, OBJECT_ID, ENTITY_ID);
    }

    @Test
    @DisplayName("Gateway маппит 404 от bo service в business not found ошибку")
    void given404FromClient_whenGetObject_thenThrowNotFoundCrmException() throws Exception {
        willThrow(feignError(404, "Not Found"))
            .given(coreDataClient)
            .getObject(TENANT_ID, USER_ID, OBJECT_ID, ENTITY_ID);

        assertThatThrownBy(() -> systemUnderTest.getObject(TENANT_ID, USER_ID, ENTITY_ID, OBJECT_ID))
            .isInstanceOf(NotFoundCrmException.class)
            .hasMessage(DocumentConstants.ErrorCodes.GENERATION_BUSINESS_OBJECT_NOT_FOUND);
    }

    @Test
    @DisplayName("Gateway маппит 5xx от core service в retriable core_client.request_failed")
    void given5xxFromClient_whenGetObject_thenThrowRetriableCoreClientException() throws Exception {
        willThrow(feignError(500, "Internal Error"))
            .given(coreDataClient)
            .getObject(TENANT_ID, USER_ID, OBJECT_ID, ENTITY_ID);

        assertThatThrownBy(() -> systemUnderTest.getObject(TENANT_ID, USER_ID, ENTITY_ID, OBJECT_ID))
            .isInstanceOf(SystemCrmException.class)
            .hasMessage(CrmErrorCodes.CORE_CLIENT_REQUEST_FAILED);
    }

    @Test
    @DisplayName("Gateway маппит retryable transport ошибку в retriable core_client.request_failed")
    void givenRetryableException_whenGetObject_thenThrowRetriableCoreClientException() throws Exception {
        willThrow(retryableError("timeout"))
            .given(coreDataClient)
            .getObject(TENANT_ID, USER_ID, OBJECT_ID, ENTITY_ID);

        assertThatThrownBy(() -> systemUnderTest.getObject(TENANT_ID, USER_ID, ENTITY_ID, OBJECT_ID))
            .isInstanceOf(SystemCrmException.class)
            .hasMessage(CrmErrorCodes.CORE_CLIENT_REQUEST_FAILED);
    }

    @Test
    @DisplayName("Gateway маппит IOException от core client в retriable core_client.request_failed")
    void givenIoException_whenGetObject_thenThrowRetriableCoreClientException() throws Exception {
        willThrow(new IOException("connection reset"))
            .given(coreDataClient)
            .getObject(TENANT_ID, USER_ID, OBJECT_ID, ENTITY_ID);

        assertThatThrownBy(() -> systemUnderTest.getObject(TENANT_ID, USER_ID, ENTITY_ID, OBJECT_ID))
            .isInstanceOf(SystemCrmException.class)
            .hasMessage(CrmErrorCodes.CORE_CLIENT_REQUEST_FAILED);
    }

    @Test
    @DisplayName("Gateway маппит 4xx от core service (кроме 404) в system unexpected")
    void given4xxFromClient_whenGetObject_thenThrowSystemCrmException() throws Exception {
        willThrow(feignError(400, "Bad Request"))
            .given(coreDataClient)
            .getObject(TENANT_ID, USER_ID, OBJECT_ID, ENTITY_ID);

        assertThatThrownBy(() -> systemUnderTest.getObject(TENANT_ID, USER_ID, ENTITY_ID, OBJECT_ID))
            .isInstanceOf(SystemCrmException.class)
            .hasMessage(CrmErrorCodes.SYSTEM_UNEXPECTED);
    }

    private FeignException feignError(int status, String reason) {
        return FeignException.errorStatus(
            "getObject",
            feign.Response.builder()
                .status(status)
                .reason(reason)
                .request(
                    feign.Request.create(
                        feign.Request.HttpMethod.GET,
                        "http://localhost/internal/data/" + ENTITY_ID + "/" + OBJECT_ID,
                        Map.of(),
                        new byte[0],
                        StandardCharsets.UTF_8,
                        null
                    )
                )
                .headers(Map.of())
                .build()
        );
    }

    private RetryableException retryableError(String message) {
        return new RetryableException(
            503,
            message,
            feign.Request.HttpMethod.GET,
            new SocketTimeoutException(message),
            new Date().getTime(),
            feign.Request.create(
                feign.Request.HttpMethod.GET,
                "http://localhost/internal/data/" + ENTITY_ID + "/" + OBJECT_ID,
                Map.of(),
                new byte[0],
                StandardCharsets.UTF_8,
                null
            )
        );
    }
}
