package ru.sberbank.sbercrm.saas.doctemplate.document.service.context;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.CollectionDataset;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.CollectionQueryKey;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.TemplateMapping;

public interface CollectionDatasetResolver {
    boolean supports(TemplateMapping mapping);

    CollectionQueryKey getQueryKey(TemplateMapping mapping);

    CollectionDataset resolve(List<TemplateMapping> mappings, Map<String, Object> sourceObject, UUID tenantId, UUID userId);
}
