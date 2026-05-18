package ru.sberbank.sbercrm.saas.doctemplate.document.service.context;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.CollectionDataset;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.CollectionQueryKey;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.TemplateMapping;

/**
 * Собирает один dataset для collection placeholder-ов с одинаковым источником данных.
 *
 * <p>Dataset затем используется DOCX/XLSX processor-ами для размножения строк таблиц.
 */
public interface CollectionDatasetResolver {
    /**
     * Проверяет, может ли обработчик обслужить mapping с конкретным типом source.
     */
    boolean supports(TemplateMapping mapping);

    /**
     * Возвращает ключ группировки mapping-ов, которые можно загружать одним запросом.
     */
    CollectionQueryKey getQueryKey(TemplateMapping mapping);

    /**
     * Загружает строки коллекции и возвращает значения, разложенные по ключам placeholder-ов.
     */
    CollectionDataset resolve(List<TemplateMapping> mappings, Map<String, Object> sourceObject, UUID tenantId, UUID userId);
}
