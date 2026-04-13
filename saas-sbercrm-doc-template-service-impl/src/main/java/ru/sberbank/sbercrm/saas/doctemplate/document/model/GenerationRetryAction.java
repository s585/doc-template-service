package ru.sberbank.sbercrm.saas.doctemplate.document.model;

public enum GenerationRetryAction {
    RETRY_NOW,
    RETRY_LATER,
    FAIL_FINAL;

    public boolean isRetry() {
        return this == RETRY_NOW || this == RETRY_LATER;
    }
}
