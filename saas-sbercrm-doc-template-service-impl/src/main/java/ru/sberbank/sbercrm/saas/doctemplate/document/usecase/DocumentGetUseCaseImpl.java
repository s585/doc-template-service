package ru.sberbank.sbercrm.saas.doctemplate.document.usecase;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.sberbank.sbercrm.saas.doctemplate.application.exception.model.NotFoundCrmException;
import ru.sberbank.sbercrm.saas.doctemplate.document.constant.DocumentConstants;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.Document;
import ru.sberbank.sbercrm.saas.doctemplate.document.service.DocumentQueryService;

@Component
@RequiredArgsConstructor
public class DocumentGetUseCaseImpl implements DocumentGetUseCase {
    private final DocumentQueryService documentQueryService;

    @Override
    public Document execute(UUID tenantId, UUID documentId) {
        return documentQueryService.findById(tenantId, documentId)
            .orElseThrow(() -> new NotFoundCrmException(
                DocumentConstants.ErrorCodes.DOCUMENT_NOT_FOUND,
                DocumentConstants.ErrorCodes.DOCUMENT_NOT_FOUND,
                documentId
            ));
    }
}
