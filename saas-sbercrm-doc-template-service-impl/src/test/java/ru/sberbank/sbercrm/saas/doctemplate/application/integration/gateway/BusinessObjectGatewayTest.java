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
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
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
import ru.sberbank.sbercrm.saas.doctemplate.shared.dto.CheckDataByEachFilterRsDto;
import ru.sberbank.sbercrm.saas.doctemplate.shared.dto.FilterDto;
import ru.sberbank.sbercrm.saas.doctemplate.shared.dto.PagingRqDto;
import ru.sberbank.sbercrm.saas.doctemplate.shared.dto.PagingRsDto;
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
    @DisplayName("Gateway сохраняет paging из ответа list-objects")
    void givenListObjectsResponseWithPaging_whenGetListObjectsPage_thenReturnPaging() {
        CommonRqDto request = CommonRqDto.builder().paging(PagingRqDto.builder().page(0).size(100).build()).build();
        PagingRsDto paging = PagingRsDto.builder()
            .currentPage(1L)
            .recordsOnPage(0L)
            .totalPageAmount(1L)
            .totalRecordsAmount(0L)
            .build();
        CommonRsDto response = CommonRsDto.builder()
            .data(null)
            .paging(paging)
            .build();
        given(coreDataClient.getListObjectsV3(TENANT_ID, USER_ID, ENTITY_ID, request)).willReturn(response);

        var actual = systemUnderTest.getListObjectsPage(TENANT_ID, USER_ID, ENTITY_ID, request);

        assertThat(actual.getData()).isEmpty();
        assertThat(actual.getPaging()).isEqualTo(paging);
    }

    @Test
    @DisplayName("Gateway возвращает пустую страницу при null ответе list-objects")
    void givenNullListObjectsResponse_whenGetListObjectsPage_thenReturnEmptyPage() {
        CommonRqDto request = CommonRqDto.builder().paging(PagingRqDto.builder().page(0).size(100).build()).build();
        given(coreDataClient.getListObjectsV3(TENANT_ID, USER_ID, ENTITY_ID, request)).willReturn(null);

        var actual = systemUnderTest.getListObjectsPage(TENANT_ID, USER_ID, ENTITY_ID, request);

        assertThat(actual.getData()).isEmpty();
        assertThat(actual.getPaging()).isNull();
    }

    @Test
    @DisplayName("Gateway падает, если data в list-objects ответе не является списком")
    void givenNonListData_whenGetListObjects_thenThrowSystemCrmException() {
        CommonRqDto request = CommonRqDto.builder().paging(PagingRqDto.builder().page(0).size(100).build()).build();
        CommonRsDto response = CommonRsDto.builder()
            .data(Map.of("id", "1"))
            .build();
        given(coreDataClient.getListObjectsV3(TENANT_ID, USER_ID, ENTITY_ID, request)).willReturn(response);

        assertThatThrownBy(() -> systemUnderTest.getListObjects(TENANT_ID, USER_ID, ENTITY_ID, request))
            .isInstanceOf(SystemCrmException.class)
            .hasMessage(CrmErrorCodes.SYSTEM_UNEXPECTED);
    }

    @Test
    @DisplayName("Gateway падает, если элемент data в list-objects ответе не является объектом")
    void givenNonMapListItem_whenGetListObjects_thenThrowSystemCrmException() {
        CommonRqDto request = CommonRqDto.builder().paging(PagingRqDto.builder().page(0).size(100).build()).build();
        CommonRsDto response = CommonRsDto.builder()
            .data(List.of("not-an-object"))
            .build();
        given(coreDataClient.getListObjectsV3(TENANT_ID, USER_ID, ENTITY_ID, request)).willReturn(response);

        assertThatThrownBy(() -> systemUnderTest.getListObjects(TENANT_ID, USER_ID, ENTITY_ID, request))
            .isInstanceOf(SystemCrmException.class)
            .hasMessage(CrmErrorCodes.SYSTEM_UNEXPECTED);
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
        List<CheckDataByEachFilterRsDto> expected = List.of(CheckDataByEachFilterRsDto.builder().result(true).build());
        CheckDataByFilterRqDto expectedRequest = CheckDataByFilterRqDto.builder()
            .data(data)
            .filter(List.of(filter))
            .build();
        given(coreDataClient.checkDataByEachFilter(TENANT_ID, USER_ID, ENTITY_ID, expectedRequest))
            .willReturn(expected);

        List<CheckDataByEachFilterRsDto> actual = systemUnderTest.checkDataByEachFilter(
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
        List<CheckDataByEachFilterRsDto> actual = systemUnderTest.checkDataByEachFilter(
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
    @DisplayName("Gateway маппит retryable transport ошибку batch-проверки в core_client.request_failed")
    void givenRetryableException_whenCheckDataByEachFilter_thenThrowRetriableCoreClientException() {
        FilterDto filter = FilterDto.builder()
            .field("source.status")
            .operation(FilterDto.Operation.EQUAL)
            .value(List.of("APPROVED"))
            .build();
        CheckDataByFilterRqDto expectedRequest = CheckDataByFilterRqDto.builder()
            .data(Map.of())
            .filter(List.of(filter))
            .build();
        willThrow(retryableError("timeout"))
            .given(coreDataClient)
            .checkDataByEachFilter(TENANT_ID, USER_ID, ENTITY_ID, expectedRequest);

        assertThatThrownBy(() -> systemUnderTest.checkDataByEachFilter(TENANT_ID, USER_ID, ENTITY_ID, Map.of(), List.of(filter)))
            .isInstanceOf(SystemCrmException.class)
            .hasMessage(CrmErrorCodes.CORE_CLIENT_REQUEST_FAILED);
    }

    @ParameterizedTest(name = "HTTP {0} -> {2}")
    @MethodSource("feignStatusMappings")
    @DisplayName("Gateway маппит HTTP ошибки batch-проверки в ожидаемые CRM ошибки")
    void givenFeignError_whenCheckDataByEachFilter_thenThrowExpectedCrmException(
        int status,
        String reason,
        String expectedErrorCode
    ) {
        FilterDto filter = FilterDto.builder()
            .field("source.status")
            .operation(FilterDto.Operation.EQUAL)
            .value(List.of("APPROVED"))
            .build();
        CheckDataByFilterRqDto expectedRequest = CheckDataByFilterRqDto.builder()
            .data(Map.of())
            .filter(List.of(filter))
            .build();
        willThrow(feignError(status, reason))
            .given(coreDataClient)
            .checkDataByEachFilter(TENANT_ID, USER_ID, ENTITY_ID, expectedRequest);

        assertThatThrownBy(() -> systemUnderTest.checkDataByEachFilter(TENANT_ID, USER_ID, ENTITY_ID, Map.of(), List.of(filter)))
            .isInstanceOf(SystemCrmException.class)
            .hasMessage(expectedErrorCode);
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

    @ParameterizedTest(name = "HTTP {0} -> {2}")
    @MethodSource("feignStatusMappings")
    @DisplayName("Gateway маппит HTTP ошибки получения объекта в ожидаемые CRM ошибки")
    void givenFeignError_whenGetObject_thenThrowExpectedCrmException(
        int status,
        String reason,
        String expectedErrorCode
    ) throws Exception {
        SelectDto fullSelect = SelectDto.builder().fields(java.util.Set.of("*")).build();
        willThrow(feignError(status, reason))
            .given(coreDataClient)
            .getObjectWithSpecifiedFieldsInternal(TENANT_ID, USER_ID, OBJECT_ID, ENTITY_ID, fullSelect);

        assertThatThrownBy(() -> systemUnderTest.getObject(TENANT_ID, USER_ID, ENTITY_ID, OBJECT_ID))
            .isInstanceOf(SystemCrmException.class)
            .hasMessage(expectedErrorCode);
    }

    private static Stream<Arguments> feignStatusMappings() {
        return Stream.of(
            Arguments.of(408, "Request Timeout", CrmErrorCodes.CORE_CLIENT_REQUEST_FAILED),
            Arguments.of(429, "Too Many Requests", CrmErrorCodes.CORE_CLIENT_REQUEST_FAILED),
            Arguments.of(500, "Internal Error", CrmErrorCodes.CORE_CLIENT_REQUEST_FAILED),
            Arguments.of(400, "Bad Request", CrmErrorCodes.SYSTEM_UNEXPECTED),
            Arguments.of(42, "Invalid Status", CrmErrorCodes.SYSTEM_UNEXPECTED)
        );
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
