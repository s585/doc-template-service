package ru.sberbank.sbercrm.doctemplate.template.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PathVariable;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import ru.sberbank.sbercrm.doctemplate.api.TemplateController;
import ru.sberbank.sbercrm.doctemplate.common.constant.CrmErrorCodes;
import ru.sberbank.sbercrm.doctemplate.common.exception.BusinessCrmException;
import ru.sberbank.sbercrm.doctemplate.common.CommonRqDto;
import ru.sberbank.sbercrm.doctemplate.common.CommonRsDto;
import ru.sberbank.sbercrm.doctemplate.template.TemplateCreationRq;
import ru.sberbank.sbercrm.doctemplate.template.TemplateRs;
import ru.sberbank.sbercrm.doctemplate.template.TemplateUpdateRq;
import ru.sberbank.sbercrm.doctemplate.template.adapter.TemplateWebAdapter;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class TemplateControllerImpl implements TemplateController {
    private static final String TENANT_ID_HEADER = "X-Tenant-Id";
    private static final String USER_ID_HEADER = "X-User-Id";

    private final TemplateWebAdapter templateWebAdapter;
    private final HttpServletRequest httpServletRequest;

    @Override
    public TemplateRs importTemplate(TemplateCreationRq request, MultipartFile file) {
        return templateWebAdapter.importTemplate(
            getRequiredUuidHeader(TENANT_ID_HEADER),
            getRequiredUuidHeader(USER_ID_HEADER),
            request,
            file
        );
    }

    @Override
    public TemplateRs updateTemplate(@PathVariable("templateId") UUID templateId, TemplateUpdateRq request) {
        return templateWebAdapter.updateTemplate(
            getRequiredUuidHeader(TENANT_ID_HEADER),
            getRequiredUuidHeader(USER_ID_HEADER),
            templateId,
            request
        );
    }

    @Override
    public void deleteTemplate(@PathVariable("templateId") UUID templateId) {
        templateWebAdapter.deleteTemplate(
            getRequiredUuidHeader(TENANT_ID_HEADER),
            getRequiredUuidHeader(USER_ID_HEADER),
            templateId
        );
    }

    @Override
    public CommonRsDto listTemplates(CommonRqDto request) {
        return templateWebAdapter.listTemplates(getRequiredUuidHeader(TENANT_ID_HEADER), request);
    }

    @Override
    public CommonRsDto listAvailableTemplates(UUID entityId, UUID objectId) {
        throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED, "List available templates is not implemented yet");
    }

    private UUID getRequiredUuidHeader(String headerName) {
        String value = httpServletRequest.getHeader(headerName);
        if (value == null || value.isBlank()) {
            throw new BusinessCrmException(CrmErrorCodes.REQUEST_HEADER_MISSING, headerName);
        }

        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            throw new BusinessCrmException(ex, CrmErrorCodes.REQUEST_HEADER_INVALID, headerName);
        }
    }
}
