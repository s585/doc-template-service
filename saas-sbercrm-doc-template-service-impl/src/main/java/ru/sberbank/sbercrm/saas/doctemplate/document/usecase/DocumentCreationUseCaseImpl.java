package ru.sberbank.sbercrm.saas.doctemplate.document.usecase;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.sberbank.sbercrm.saas.doctemplate.application.exception.model.NotFoundCrmException;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.Document;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.DocumentCreationCmd;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.GeneratedDocument;
import ru.sberbank.sbercrm.saas.doctemplate.document.service.GeneratedDocumentService;
import ru.sberbank.sbercrm.saas.doctemplate.document.service.GeneratedFileService;
import ru.sberbank.sbercrm.saas.doctemplate.document.service.GenerationJobService;
import ru.sberbank.sbercrm.saas.doctemplate.template.constant.TemplateConstants;
import ru.sberbank.sbercrm.saas.doctemplate.template.service.TemplateService;

@Component
@RequiredArgsConstructor
public class DocumentCreationUseCaseImpl implements DocumentCreationUseCase {
    private final GeneratedDocumentService generatedDocumentService;
    private final GeneratedFileService generatedFileService;
    private final GenerationJobService generationJobService;
    private final DocumentGetUseCase documentGetUseCase;
    private final TemplateService templateService;

    @Override
    @Transactional
    public Document execute(UUID tenantId, UUID userId, DocumentCreationCmd command) {
        if (!templateService.exists(tenantId, command.getTemplateId())) {
            throw new NotFoundCrmException(
                TemplateConstants.ErrorCodes.TEMPLATE_NOT_FOUND,
                TemplateConstants.ErrorCodes.TEMPLATE_NOT_FOUND,
                command.getTemplateId()
            );
        }

        GeneratedDocument document = generatedDocumentService.findByRequestId(tenantId, command.getRequestId())
            .orElseGet(() -> createDocument(tenantId, userId, command));

        return documentGetUseCase.execute(tenantId, document.getId());
    }

    private GeneratedDocument createDocument(UUID tenantId, UUID userId, DocumentCreationCmd command) {
        GeneratedDocument document = generatedDocumentService.create(tenantId, userId, command);
        generatedFileService.createAll(tenantId, userId, document.getId(), command.getFormats());
        generationJobService.createAll(tenantId, userId, document.getId(), command);
        return document;
    }
}
