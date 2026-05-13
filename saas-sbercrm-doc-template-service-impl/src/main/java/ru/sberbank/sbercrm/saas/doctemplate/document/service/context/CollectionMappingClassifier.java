package ru.sberbank.sbercrm.saas.doctemplate.document.service.context;

import ru.sberbank.sbercrm.saas.doctemplate.document.model.CollectionQueryKey;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.TemplateMapping;

public interface CollectionMappingClassifier {
    boolean supports(TemplateMapping mapping);

    CollectionQueryKey getQueryKey(TemplateMapping mapping);
}
