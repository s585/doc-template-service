package ru.sberbank.sbercrm.saas.doctemplate.application.integration.gateway;

import feign.FeignException;
import feign.RetryableException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import ru.sberbank.sbercrm.saas.doctemplate.application.exception.CrmErrorCodes;
import ru.sberbank.sbercrm.saas.doctemplate.application.exception.model.NotFoundCrmException;
import ru.sberbank.sbercrm.saas.doctemplate.application.exception.model.SystemCrmException;
import ru.sberbank.sbercrm.saas.doctemplate.application.integration.client.CoreDataClient;
import ru.sberbank.sbercrm.saas.doctemplate.document.constant.DocumentConstants;
import ru.sberbank.sbercrm.saas.doctemplate.shared.dto.CommonRqDto;
import ru.sberbank.sbercrm.saas.doctemplate.shared.dto.CommonRsDto;
import ru.sberbank.sbercrm.saas.doctemplate.shared.dto.SelectDto;

@Component
@RequiredArgsConstructor
public class BusinessObjectGateway {
    private final CoreDataClient coreDataClient;
    private static final SelectDto FULL_OBJECT_SELECT = SelectDto.builder().fields(Set.of("*")).build();

    public Map<String, Object> getObject(UUID tenantId, UUID userId, UUID entityId, UUID objectId) {
        return getObject(tenantId, userId, entityId, objectId, FULL_OBJECT_SELECT);
    }

    public Map<String, Object> getObject(UUID tenantId, UUID userId, UUID entityId, UUID objectId, SelectDto selectDto) {
        try {
            SelectDto effectiveSelect = selectDto == null ? FULL_OBJECT_SELECT : selectDto;
            return coreDataClient.getObjectWithSpecifiedFieldsInternal(tenantId, userId, objectId, entityId, effectiveSelect);
        } catch (FeignException.NotFound ex) {
            throw new NotFoundCrmException(
                DocumentConstants.ErrorCodes.GENERATION_BUSINESS_OBJECT_NOT_FOUND,
                DocumentConstants.ErrorCodes.GENERATION_BUSINESS_OBJECT_NOT_FOUND,
                entityId,
                objectId
            );
        } catch (RetryableException ex) {
            throw new SystemCrmException(
                CrmErrorCodes.CORE_CLIENT_REQUEST_FAILED,
                CrmErrorCodes.CORE_CLIENT_REQUEST_FAILED,
                ex,
                buildObjectContext(entityId, objectId)
            );
        } catch (FeignException ex) {
            if (isRetriableResponseStatus(ex.status())) {
                throw new SystemCrmException(
                    CrmErrorCodes.CORE_CLIENT_REQUEST_FAILED,
                    CrmErrorCodes.CORE_CLIENT_REQUEST_FAILED,
                    ex,
                    buildObjectContext(entityId, objectId)
                );
            }
            throw new SystemCrmException(
                CrmErrorCodes.SYSTEM_UNEXPECTED,
                CrmErrorCodes.SYSTEM_UNEXPECTED,
                ex,
                buildObjectContext(entityId, objectId)
            );
        }
    }

    public List<Map<String, Object>> getListObjects(UUID tenantId, UUID userId, UUID entityId, CommonRqDto request) {
        return getListObjectsPage(tenantId, userId, entityId, request).items();
    }

    public PagedResult<Map<String, Object>> getListObjectsPage(UUID tenantId, UUID userId, UUID entityId, CommonRqDto request) {
        try {
            CommonRsDto response = coreDataClient.getListObjectsV3(tenantId, userId, entityId, request);
            return new PagedResult<>(extractData(response, entityId), response == null ? null : response.getPaging());
        } catch (RetryableException ex) {
            throw new SystemCrmException(
                CrmErrorCodes.CORE_CLIENT_REQUEST_FAILED,
                CrmErrorCodes.CORE_CLIENT_REQUEST_FAILED,
                ex,
                "entityId=" + entityId
            );
        } catch (FeignException ex) {
            if (isRetriableResponseStatus(ex.status())) {
                throw new SystemCrmException(
                    CrmErrorCodes.CORE_CLIENT_REQUEST_FAILED,
                    CrmErrorCodes.CORE_CLIENT_REQUEST_FAILED,
                    ex,
                    "entityId=" + entityId
                );
            }
            throw new SystemCrmException(
                CrmErrorCodes.SYSTEM_UNEXPECTED,
                CrmErrorCodes.SYSTEM_UNEXPECTED,
                ex,
                "entityId=" + entityId
            );
        }
    }

    private List<Map<String, Object>> extractData(CommonRsDto response, UUID entityId) {
        Object data = response == null ? null : response.getData();
        if (data == null) {
            return List.of();
        }
        if (!(data instanceof List<?> items)) {
            throw new SystemCrmException(
                CrmErrorCodes.SYSTEM_UNEXPECTED,
                CrmErrorCodes.SYSTEM_UNEXPECTED,
                "Invalid list-objects response: entityId=" + entityId
            );
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object item : items) {
            if (!(item instanceof Map<?, ?> rawMap)) {
                throw new SystemCrmException(
                    CrmErrorCodes.SYSTEM_UNEXPECTED,
                    CrmErrorCodes.SYSTEM_UNEXPECTED,
                    "Invalid list-objects item: entityId=" + entityId
                );
            }
            result.add((Map<String, Object>) rawMap);
        }
        return result;
    }

    private boolean isRetriableResponseStatus(int status) {
        try {
            HttpStatusCode httpStatusCode = HttpStatusCode.valueOf(status);
            return httpStatusCode.is5xxServerError()
                || status == HttpStatus.TOO_MANY_REQUESTS.value()
                || status == HttpStatus.REQUEST_TIMEOUT.value();
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private String buildObjectContext(UUID entityId, UUID objectId) {
        return "entityId=" + entityId + ", objectId=" + objectId;
    }
}
