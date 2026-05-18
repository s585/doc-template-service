package ru.sberbank.sbercrm.saas.doctemplate.template.processor;

import java.util.List;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.GenerationTemplateContext;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.TemplateFormat;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.TemplateVariableInfo;

/**
 * Единая точка входа в обработку файлов шаблонов разных форматов.
 *
 * <p>Скрывает выбор конкретного processor-а по {@link TemplateFormat}, чтобы use case-ы
 * импорта и генерации не знали деталей DOCX/XLSX реализаций.
 */
public interface TemplateProcessingFacade {

    /**
     * Извлекает placeholder-ы из файла шаблона для дальнейшей настройки mapping-ов.
     */
    List<TemplateVariableInfo> extractVariables(TemplateFormat format, byte[] content);

    /**
     * Генерирует файл заданного формата, подставляя значения из {@link GenerationTemplateContext}.
     */
    byte[] generate(TemplateFormat format, byte[] content, GenerationTemplateContext context);
}
