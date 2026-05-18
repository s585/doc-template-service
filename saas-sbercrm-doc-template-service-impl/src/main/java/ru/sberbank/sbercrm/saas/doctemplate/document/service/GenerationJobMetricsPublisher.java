package ru.sberbank.sbercrm.saas.doctemplate.document.service;

import ru.sberbank.sbercrm.saas.doctemplate.document.model.GenerationRetryDecision;

/**
 * Публикует технические метрики generation worker-а.
 *
 * <p>Интерфейс намеренно отделён от бизнес-логики, чтобы execution flow мог оставаться
 * стабильным при замене конкретной системы мониторинга.
 */
public interface GenerationJobMetricsPublisher {
    /**
     * Увеличивает счётчик job, для которых retry больше не разрешён.
     */
    void incrementExhaustedRetries(GenerationRetryDecision retryDecision);

    /**
     * Увеличивает счётчик ошибок recovery timed out job.
     */
    void incrementRecoveryFailure();
}
