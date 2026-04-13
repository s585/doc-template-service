package ru.sberbank.sbercrm.saas.doctemplate.document.model;

import java.time.OffsetDateTime;

public record GenerationRetryDecision(
    GenerationRetryAction action,
    String errorCode,
    String errorMessage,
    OffsetDateTime nextRetryAt
) {
}
