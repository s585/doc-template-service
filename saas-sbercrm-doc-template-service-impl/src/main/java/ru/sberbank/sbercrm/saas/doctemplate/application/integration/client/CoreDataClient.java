package ru.sberbank.sbercrm.saas.doctemplate.application.integration.client;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import ru.sberbank.sbercrm.saas.doctemplate.application.integration.client.config.CoreClientFeignConfig;
import ru.sberbank.sbercrm.saas.doctemplate.shared.dto.*;

@FeignClient(name = "core-client", configuration = CoreClientFeignConfig.class)
public interface CoreDataClient {
    String TENANT_ID_HEADER = "X-Tenant-Id";
    String USER_ID_HEADER = "X-User-Id";

    @PostMapping("/internal/data/{entityId}/{objectId}")
    Map<String, Object> getObjectWithSpecifiedFieldsInternal(
        @RequestHeader(TENANT_ID_HEADER) UUID tenantId,
        @RequestHeader(USER_ID_HEADER) UUID userId,
        @PathVariable("objectId") UUID objectId,
        @PathVariable("entityId") UUID entityId,
        @RequestBody SelectDto selectDto
    );

    @PostMapping("/internal/data/{entityId}/list-objects")
    CommonRsDto getListObjectsV3(
        @RequestHeader(TENANT_ID_HEADER) UUID tenantId,
        @RequestHeader(USER_ID_HEADER) UUID userId,
        @PathVariable("entityId") UUID entityId,
        @RequestBody CommonRqDto commonRqDto
    );

    @PostMapping("/internal/data/{entityId}/check-each")
    List<CheckDataByEachFilterRsDto> checkDataByEachFilter(
        @RequestHeader(TENANT_ID_HEADER) UUID tenantId,
        @RequestHeader(USER_ID_HEADER) UUID userId,
        @PathVariable("entityId") UUID entityId,
        @RequestBody CheckDataByFilterRqDto rq
    );
}
