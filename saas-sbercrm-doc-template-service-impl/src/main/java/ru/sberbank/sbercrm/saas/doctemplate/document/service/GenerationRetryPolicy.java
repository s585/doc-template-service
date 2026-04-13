package ru.sberbank.sbercrm.saas.doctemplate.document.service;

import ru.sberbank.sbercrm.saas.doctemplate.document.model.GenerationErrorDecision;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.GenerationRetryDecision;

/**
 * Политика повторного запуска generation job.
 *
 * <p>Отвечает за количество попыток и расчёт времени следующего запуска.
 */
public interface GenerationRetryPolicy {
    /**
     * Принимает итоговое retry-решение для текущей попытки.
     *
     * <p>{@code attemptNo} — номер уже завершившейся или оборвавшейся попытки, для которой оценивается
     * дальнейшее действие.
     */
    GenerationRetryDecision decide(int attemptNo, GenerationErrorDecision errorDecision);
}
