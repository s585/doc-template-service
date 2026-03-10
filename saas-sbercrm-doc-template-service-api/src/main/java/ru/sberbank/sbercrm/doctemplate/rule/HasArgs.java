package ru.sberbank.sbercrm.doctemplate.rule;

import java.util.List;

/**
 * Интерфейс, уточняющий наличие у правила вложенных аргументов
 */
public interface HasArgs {
    /**
     * @return список вложенных правил-аргументов
     */
    List<RuleDto> getArgs();
}
