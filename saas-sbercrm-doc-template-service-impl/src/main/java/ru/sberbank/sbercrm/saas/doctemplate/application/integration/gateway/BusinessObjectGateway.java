package ru.sberbank.sbercrm.saas.doctemplate.application.integration.gateway;

import feign.FeignException;
import feign.RetryableException;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
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
            return coreDataClient.getObject(tenantId, userId, entityId, objectId);
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
                entityId,
                objectId
            );
        } catch (FeignException ex) {
            if (isRetriableResponseStatus(ex.status())) {
                throw new SystemCrmException(
                    CrmErrorCodes.CORE_CLIENT_REQUEST_FAILED,
                    CrmErrorCodes.CORE_CLIENT_REQUEST_FAILED,
                    ex,
                    entityId,
                    objectId
                );
            }
            throw new SystemCrmException(
                CrmErrorCodes.SYSTEM_UNEXPECTED,
                CrmErrorCodes.SYSTEM_UNEXPECTED,
                ex,
                entityId,
                objectId
            );
        }
    }

    private boolean isRetriableResponseStatus(int status) {
        return status >= 500 || status == 429 || status == 408;
    }
}
