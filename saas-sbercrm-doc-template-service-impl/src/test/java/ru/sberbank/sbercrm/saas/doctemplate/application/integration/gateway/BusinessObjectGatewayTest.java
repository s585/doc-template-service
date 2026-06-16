package ru.sberbank.sbercrm.saas.doctemplate.application.integration.gateway;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;

import feign.FeignException;
import feign.RetryableException;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.net.SocketTimeoutException;
import java.util.List;
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
import ru.sberbank.sbercrm.saas.doctemplate.shared.dto.CommonRqDto;
import ru.sberbank.sbercrm.saas.doctemplate.shared.dto.CommonRsDto;
import ru.sberbank.sbercrm.saas.doctemplate.shared.dto.CheckDataByFilterRqDto;
import ru.sberbank.sbercrm.saas.doctemplate.shared.dto.CheckDataByFilterRsDto;
import ru.sberbank.sbercrm.saas.doctemplate.shared.dto.FilterDto;
import ru.sberbank.sbercrm.saas.doctemplate.shared.dto.PagingRqDto;
import ru.sberbank.sbercrm.saas.doctemplate.shared.dto.SelectDto;

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
        SelectDto fullSelect = SelectDto.builder().fields(java.util.Set.of("*")).build();
        given(coreDataClient.getObjectWithSpecifiedFieldsInternal(TENANT_ID, USER_ID, OBJECT_ID, ENTITY_ID, fullSelect))
            .willReturn(expected);

        Map<String, Object> actual = systemUnderTest.getObject(TENANT_ID, USER_ID, ENTITY_ID, OBJECT_ID);

        assertThat(actual).isEqualTo(expected);
        verify(coreDataClient).getObjectWithSpecifiedFieldsInternal(TENANT_ID, USER_ID, OBJECT_ID, ENTITY_ID, fullSelect);
    }

    @Test
    @DisplayName("Gateway проксирует selective object fetch в feign client")
    void givenSelectDto_whenGetObject_thenUseSpecifiedFieldsEndpoint() {
        Map<String, Object> expected = Map.of("customer", Map.of("name", "BO LLC"));
        SelectDto selectDto = SelectDto.builder().fields(java.util.Set.of("customer.name")).build();
        given(coreDataClient.getObjectWithSpecifiedFieldsInternal(TENANT_ID, USER_ID, OBJECT_ID, ENTITY_ID, selectDto))
            .willReturn(expected);

        Map<String, Object> actual = systemUnderTest.getObject(TENANT_ID, USER_ID, ENTITY_ID, OBJECT_ID, selectDto);

        assertThat(actual).isEqualTo(expected);
        verify(coreDataClient).getObjectWithSpecifiedFieldsInternal(TENANT_ID, USER_ID, OBJECT_ID, ENTITY_ID, selectDto);
    }

    @Test
    @DisplayName("Gateway достает data из ответа list-objects")
    void givenListObjectsResponse_whenGetListObjects_thenReturnDataItems() {
        CommonRqDto request = CommonRqDto.builder().paging(PagingRqDto.builder().page(0).size(100).build()).build();
        CommonRsDto response = CommonRsDto.builder()
            .data(List.of(
                Map.of("id", "1", "name", "Product A"),
                Map.of("id", "2", "name", "Product B")
            ))
            .build();
        given(coreDataClient.getListObjectsV3(TENANT_ID, USER_ID, ENTITY_ID, request)).willReturn(response);

        List<Map<String, Object>> actual = systemUnderTest.getListObjects(TENANT_ID, USER_ID, ENTITY_ID, request);

        assertThat(actual).hasSize(2);
        assertThat(actual.getFirst()).containsEntry("name", "Product A");
        verify(coreDataClient).getListObjectsV3(TENANT_ID, USER_ID, ENTITY_ID, request);
    }

    @Test
    @DisplayName("Gateway проксирует batch-проверку условий в core client")
    void givenFilters_whenCheckDataByEachFilter_thenReturnClientResponse() {
        Map<String, Object> data = Map.of("source", Map.of("status", "APPROVED"));
        FilterDto filter = FilterDto.builder()
            .field("source.status")
            .operation(FilterDto.Operation.EQUAL)
            .value(List.of("APPROVED"))
            .build();
        List<CheckDataByFilterRsDto> expected = List.of(CheckDataByFilterRsDto.builder().result(true).build());
        CheckDataByFilterRqDto expectedRequest = CheckDataByFilterRqDto.builder()
            .data(data)
            .filter(List.of(filter))
            .build();
        given(coreDataClient.checkDataByEachFilter(TENANT_ID, USER_ID, ENTITY_ID, expectedRequest))
            .willReturn(expected);

        List<CheckDataByFilterRsDto> actual = systemUnderTest.checkDataByEachFilter(
            TENANT_ID,
            USER_ID,
            ENTITY_ID,
            data,
            List.of(filter)
        );

        assertThat(actual).isEqualTo(expected);
        verify(coreDataClient).checkDataByEachFilter(TENANT_ID, USER_ID, ENTITY_ID, expectedRequest);
    }

    @Test
    @DisplayName("Gateway не вызывает core client для пустого списка условий")
    void givenEmptyFilters_whenCheckDataByEachFilter_thenReturnEmptyResponse() {
        List<CheckDataByFilterRsDto> actual = systemUnderTest.checkDataByEachFilter(
            TENANT_ID,
            USER_ID,
            ENTITY_ID,
            Map.of(),
            List.of()
        );

        assertThat(actual).isEmpty();
        verify(coreDataClient, org.mockito.Mockito.never())
            .checkDataByEachFilter(org.mockito.Mockito.any(), org.mockito.Mockito.any(), org.mockito.Mockito.any(), org.mockito.Mockito.any());
    }

    @Test
    @DisplayName("Gateway маппит 404 от bo service в business not found ошибку")
    void given404FromClient_whenGetObject_thenThrowNotFoundCrmException() throws Exception {
        SelectDto fullSelect = SelectDto.builder().fields(java.util.Set.of("*")).build();
        willThrow(feignError(404, "Not Found"))
            .given(coreDataClient)
            .getObjectWithSpecifiedFieldsInternal(TENANT_ID, USER_ID, OBJECT_ID, ENTITY_ID, fullSelect);

        assertThatThrownBy(() -> systemUnderTest.getObject(TENANT_ID, USER_ID, ENTITY_ID, OBJECT_ID))
            .isInstanceOf(NotFoundCrmException.class)
            .hasMessage(DocumentConstants.ErrorCodes.GENERATION_BUSINESS_OBJECT_NOT_FOUND);
    }

    @Test
    @DisplayName("Gateway маппит 5xx от core service в retriable core_client.request_failed")
    void given5xxFromClient_whenGetObject_thenThrowRetriableCoreClientException() throws Exception {
        SelectDto fullSelect = SelectDto.builder().fields(java.util.Set.of("*")).build();
        willThrow(feignError(500, "Internal Error"))
            .given(coreDataClient)
            .getObjectWithSpecifiedFieldsInternal(TENANT_ID, USER_ID, OBJECT_ID, ENTITY_ID, fullSelect);

        assertThatThrownBy(() -> systemUnderTest.getObject(TENANT_ID, USER_ID, ENTITY_ID, OBJECT_ID))
            .isInstanceOf(SystemCrmException.class)
            .hasMessage(CrmErrorCodes.CORE_CLIENT_REQUEST_FAILED);
    }

    @Test
    @DisplayName("Gateway маппит retryable transport ошибку в retriable core_client.request_failed")
    void givenRetryableException_whenGetObject_thenThrowRetriableCoreClientException() throws Exception {
        SelectDto fullSelect = SelectDto.builder().fields(java.util.Set.of("*")).build();
        willThrow(retryableError("timeout"))
            .given(coreDataClient)
            .getObjectWithSpecifiedFieldsInternal(TENANT_ID, USER_ID, OBJECT_ID, ENTITY_ID, fullSelect);

        assertThatThrownBy(() -> systemUnderTest.getObject(TENANT_ID, USER_ID, ENTITY_ID, OBJECT_ID))
            .isInstanceOf(SystemCrmException.class)
            .hasMessage(CrmErrorCodes.CORE_CLIENT_REQUEST_FAILED);
    }

    @Test
    @DisplayName("Gateway маппит 4xx от core service (кроме 404) в system unexpected")
    void given4xxFromClient_whenGetObject_thenThrowSystemCrmException() throws Exception {
        SelectDto fullSelect = SelectDto.builder().fields(java.util.Set.of("*")).build();
        willThrow(feignError(400, "Bad Request"))
            .given(coreDataClient)
            .getObjectWithSpecifiedFieldsInternal(TENANT_ID, USER_ID, OBJECT_ID, ENTITY_ID, fullSelect);

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
