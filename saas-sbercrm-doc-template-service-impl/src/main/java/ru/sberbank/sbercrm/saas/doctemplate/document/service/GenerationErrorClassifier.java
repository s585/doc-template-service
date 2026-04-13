package ru.sberbank.sbercrm.saas.doctemplate.document.service;

import ru.sberbank.sbercrm.saas.doctemplate.document.model.GenerationErrorDecision;

/**
 * Классификатор ошибок generation flow.
 *
 * <p>Нормализует исключение в код ошибки и признак retriable/non-retriable.
 */
public interface GenerationErrorClassifier {
    /**
     * Классифицирует runtime-исключение из generation pipeline.
     */
    GenerationErrorDecision classify(Throwable throwable);

    /**
     * Классифицирует уже нормализованный код ошибки.
     */
    GenerationErrorDecision classify(String errorCode, String errorMessage);
}
