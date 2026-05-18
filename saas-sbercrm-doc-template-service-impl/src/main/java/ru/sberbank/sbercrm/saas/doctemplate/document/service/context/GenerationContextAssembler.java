package ru.sberbank.sbercrm.saas.doctemplate.document.service.context;

import java.util.UUID;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.GenerationJob;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.GenerationTemplateContext;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.Template;

/**
 * Собирает runtime-контекст, который нужен процессору шаблонов для подстановки значений.
 *
 * <p>Контекст содержит скалярные значения, табличные datasets для collection placeholder-ов
 * и итоговое имя генерируемого файла. Реализация не должна протаскивать в логи сами значения
 * полей бизнес-объекта: это диагностический, но потенциально чувствительный payload.
 */
public interface GenerationContextAssembler {
    /**
     * Загружает необходимые данные бизнес-объекта и раскладывает mapping-и шаблона в контекст генерации.
     */
    GenerationTemplateContext assemble(GenerationJob job, UUID userId, Template template);
}
