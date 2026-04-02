package ru.sberbank.sbercrm.saas.doctemplate.document.converter;

import org.mapstruct.Mapper;
import ru.sberbank.sbercrm.saas.doctemplate.document.dto.DocumentCreationRq;
import ru.sberbank.sbercrm.saas.doctemplate.document.dto.DocumentRs;
import ru.sberbank.sbercrm.saas.doctemplate.document.dto.GeneratedFileRs;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.Document;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.DocumentCreationCmd;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.GeneratedFile;

@Mapper(componentModel = "spring")
public interface DocumentConverter {
    DocumentCreationCmd convertToModel(DocumentCreationRq request);

    DocumentRs convertToRs(Document document);

    GeneratedFileRs convertToRs(GeneratedFile file);
}
