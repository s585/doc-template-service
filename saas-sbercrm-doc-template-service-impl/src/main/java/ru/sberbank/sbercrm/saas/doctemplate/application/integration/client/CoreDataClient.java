package ru.sberbank.sbercrm.saas.doctemplate.application.integration.client;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import ru.sberbank.sbercrm.saas.doctemplate.application.integration.client.config.CoreClientFeignConfig;

@FeignClient(name = "core-client", configuration = CoreClientFeignConfig.class)
public interface CoreDataClient {
    String TENANT_ID_HEADER = "X-Tenant-Id";
    String USER_ID_HEADER = "X-User-Id";

    @GetMapping("/internal/data/{entityId}/{objectId}")
    Map<String, Object> getObject(
        @RequestHeader(TENANT_ID_HEADER) UUID tenantId,
        @RequestHeader(USER_ID_HEADER) UUID userId,
        @RequestParam("objectId") UUID objectId,
        @RequestParam("entityId") UUID entityId
    ) throws IOException;
}
