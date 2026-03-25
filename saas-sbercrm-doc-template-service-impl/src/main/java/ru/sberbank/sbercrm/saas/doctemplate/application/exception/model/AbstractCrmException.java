package ru.sberbank.sbercrm.saas.doctemplate.application.exception.model;

import lombok.Getter;

@Getter
public abstract class AbstractCrmException extends RuntimeException {
    private final String code;
    private final Object[] params;

    protected AbstractCrmException(String code, Object... params) {
        super(code);
        this.code = code;
        this.params = params;
    }

    protected AbstractCrmException(Throwable cause, String code, Object... params) {
        super(code, cause);
        this.code = code;
        this.params = params;
    }
}
