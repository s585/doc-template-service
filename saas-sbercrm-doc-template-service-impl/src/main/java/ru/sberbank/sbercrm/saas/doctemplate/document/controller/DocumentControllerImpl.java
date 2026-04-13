package ru.sberbank.sbercrm.saas.doctemplate.document.controller;

import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import ru.sberbank.sbercrm.saas.doctemplate.application.exception.CrmErrorCodes;
import ru.sberbank.sbercrm.saas.doctemplate.application.exception.model.BusinessCrmException;
import ru.sberbank.sbercrm.saas.doctemplate.document.adapter.DocumentWebAdapter;
import ru.sberbank.sbercrm.saas.doctemplate.document.dto.DocumentCreationRq;
import ru.sberbank.sbercrm.saas.doctemplate.document.dto.DocumentRs;
import ru.sberbank.sbercrm.saas.doctemplate.shared.dto.CommonRqDto;
import ru.sberbank.sbercrm.saas.doctemplate.shared.dto.CommonRsDto;

@RestController
@RequiredArgsConstructor
public class DocumentControllerImpl implements DocumentController {
    private static final String TENANT_ID_HEADER = "X-Tenant-Id";
    private static final String USER_ID_HEADER = "X-User-Id";

    private final DocumentWebAdapter documentWebAdapter;
    private final HttpServletRequest httpServletRequest;

    @Override
    public DocumentRs createDocument(DocumentCreationRq request) {
        return documentWebAdapter.createDocument(
            getRequiredUuidHeader(TENANT_ID_HEADER),
            getRequiredUuidHeader(USER_ID_HEADER),
            request
        );
    }

    @Override
    public DocumentRs getDocument(@PathVariable("documentId") UUID documentId) {
        return documentWebAdapter.getDocument(
            getRequiredUuidHeader(TENANT_ID_HEADER),
            documentId
        );
    }

    @Override
    public CommonRsDto listDocuments(UUID entityId, UUID objectId, CommonRqDto request) {
        return documentWebAdapter.listDocuments(
            getRequiredUuidHeader(TENANT_ID_HEADER),
            entityId,
            objectId,
            request
        );
    }

    private UUID getRequiredUuidHeader(String headerName) {
        String value = httpServletRequest.getHeader(headerName);
        if (value == null || value.isBlank()) {
            throw new BusinessCrmException(CrmErrorCodes.REQUEST_HEADER_MISSING, CrmErrorCodes.REQUEST_HEADER_MISSING, headerName);
        }

        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            throw new BusinessCrmException(CrmErrorCodes.REQUEST_HEADER_INVALID, CrmErrorCodes.REQUEST_HEADER_INVALID, ex, headerName);
        }
    }
}
