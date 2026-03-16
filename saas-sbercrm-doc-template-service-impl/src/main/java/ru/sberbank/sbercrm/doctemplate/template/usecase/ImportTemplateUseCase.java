package ru.sberbank.sbercrm.doctemplate.template.usecase;

import org.springframework.web.multipart.MultipartFile;
import ru.sberbank.sbercrm.doctemplate.template.model.TemplateCreationCmd;
import ru.sberbank.sbercrm.doctemplate.template.model.Template;

import java.util.UUID;

public interface ImportTemplateUseCase {
    Template execute(UUID tenantId, UUID userId, TemplateCreationCmd request, MultipartFile file);
}
