package ru.sberbank.sbercrm.saas.doctemplate.document.service.context;

import java.util.Map;
import java.util.UUID;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.TemplateMapping;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.source.ValueSource;

/**
 * Извлекает значение одного mapping-а из runtime-источника.
 *
 * <p>Разные реализации обслуживают разные {@link ValueSource}: прямое поле текущего объекта,
 * ссылочный объект или константу.
 */
public interface MappingValueResolver {
    /**
     * Проверяет, поддерживает ли обработчик конкретный тип source.
     */
    boolean supports(ValueSource source);

    /**
     * Возвращает сырое значение mapping-а; expression evaluation выполняется выше по цепочке.
     */
    Object resolve(TemplateMapping mapping, ValueSource source, Map<String, Object> sourceObject, UUID tenantId, UUID userId);
}
