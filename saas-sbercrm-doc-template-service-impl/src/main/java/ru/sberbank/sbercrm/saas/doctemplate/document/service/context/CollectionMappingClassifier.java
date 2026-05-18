package ru.sberbank.sbercrm.saas.doctemplate.document.service.context;

import ru.sberbank.sbercrm.saas.doctemplate.document.model.CollectionQueryKey;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.TemplateMapping;

/**
 * Определяет, можно ли mapping обработать как collection mapping, и возвращает ключ его группировки.
 *
 * <p>Используется planner-ом до фактической загрузки данных, чтобы разнести mapping-и на scalar
 * и collection части generation context.
 */
public interface CollectionMappingClassifier {
    /**
     * Возвращает {@code true}, если mapping можно включить в collection dataset.
     */
    boolean supports(TemplateMapping mapping);

    /**
     * Возвращает ключ, по которому совместимые mapping-и группируются в один запрос.
     */
    CollectionQueryKey getQueryKey(TemplateMapping mapping);
}
