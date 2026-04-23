package ru.sberbank.sbercrm.saas.doctemplate.application.integration.gateway;

import feign.FeignException;
import feign.RetryableException;
import java.io.IOException;
import java.util.Map;
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

@Component
@RequiredArgsConstructor
public class BusinessObjectGateway {
    private final CoreDataClient coreDataClient;

    public Map<String, Object> getObject(UUID tenantId, UUID userId, UUID entityId, UUID objectId) {
        try {
            return coreDataClient.getObject(tenantId, userId, objectId, entityId);
        } catch (FeignException.NotFound ex) {
            throw new NotFoundCrmException(
                DocumentConstants.ErrorCodes.GENERATION_BUSINESS_OBJECT_NOT_FOUND,
                DocumentConstants.ErrorCodes.GENERATION_BUSINESS_OBJECT_NOT_FOUND,
                entityId,
                objectId
            );
        } catch (IOException | RetryableException ex) {
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
