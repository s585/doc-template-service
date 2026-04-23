package ru.sberbank.sbercrm.saas.doctemplate.document.service;

import ru.sberbank.sbercrm.saas.doctemplate.document.model.GenerationRetryDecision;

public interface GenerationJobMetricsPublisher {
    void incrementExhaustedRetries(GenerationRetryDecision retryDecision);

    void incrementRecoveryFailure();
}
