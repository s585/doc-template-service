package ru.sberbank.sbercrm.saas.doctemplate.document.usecase;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.sberbank.sbercrm.saas.doctemplate.application.exception.model.NotFoundCrmException;
import ru.sberbank.sbercrm.saas.doctemplate.document.constant.DocumentConstants;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.Document;
import ru.sberbank.sbercrm.saas.doctemplate.document.repository.DocumentQueryRepository;

@Component
@RequiredArgsConstructor
public class DocumentGetUseCaseImpl implements DocumentGetUseCase {
    private final DocumentQueryRepository documentQueryRepository;

    @Override
    public Document execute(UUID tenantId, UUID documentId) {
        return documentQueryRepository.findById(tenantId, documentId)
            .orElseThrow(() -> new NotFoundCrmException(DocumentConstants.ErrorCodes.DOCUMENT_NOT_FOUND, documentId));
    }
}
