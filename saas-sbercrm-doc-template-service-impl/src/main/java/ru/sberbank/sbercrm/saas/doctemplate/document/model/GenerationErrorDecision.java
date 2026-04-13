package ru.sberbank.sbercrm.saas.doctemplate.document.model;

public record GenerationErrorDecision(
    String errorCode,
    String errorMessage,
    boolean retriable
) {
}
