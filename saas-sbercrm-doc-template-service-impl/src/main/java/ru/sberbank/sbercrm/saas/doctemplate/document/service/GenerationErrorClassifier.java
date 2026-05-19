package ru.sberbank.sbercrm.saas.doctemplate.document.service;

import org.checkerframework.checker.nullness.qual.Nullable;
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
    GenerationErrorDecision classify(@Nullable Throwable throwable);

    /**
     * Классифицирует уже нормализованный код ошибки.
     */
    GenerationErrorDecision classify(@Nullable String errorCode, @Nullable String errorMessage);
}
