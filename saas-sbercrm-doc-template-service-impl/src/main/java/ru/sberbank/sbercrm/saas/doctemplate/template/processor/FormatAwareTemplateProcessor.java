package ru.sberbank.sbercrm.saas.doctemplate.template.processor;

import ru.sberbank.sbercrm.saas.doctemplate.document.model.GenerationTemplateContext;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.TemplateFormat;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.TemplateVariableInfo;

import java.util.List;

/**
 * Низкоуровневый processor конкретного формата шаблона.
 *
 * <p>Реализация знает внутреннюю структуру файла своего формата и отвечает за извлечение
 * placeholder-ов и генерацию результата без изменения исходного массива шаблона.
 */
public interface FormatAwareTemplateProcessor {
    /**
     * Возвращает {@code true}, если processor умеет работать с переданным форматом.
     */
    boolean supports(TemplateFormat format);

    /**
     * Находит переменные, доступные для настройки mapping-ов при импорте шаблона.
     */
    List<TemplateVariableInfo> extractVariables(byte[] content);

    /**
     * Возвращает новый файл с подставленными скалярными значениями и развернутыми коллекциями.
     */
    byte[] generate(byte[] content, GenerationTemplateContext context);
}
