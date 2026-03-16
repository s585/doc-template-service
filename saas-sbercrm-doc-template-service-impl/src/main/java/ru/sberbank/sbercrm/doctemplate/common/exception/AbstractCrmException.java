package ru.sberbank.sbercrm.doctemplate.common.exception;

import lombok.Getter;

import java.io.Serializable;

@Getter
public abstract class AbstractCrmException extends RuntimeException {
    private final String code;
    private final Serializable[] params;

    protected AbstractCrmException(String code, Serializable... params) {
        super(code);
        this.code = code;
        this.params = params;
    }

    protected AbstractCrmException(Throwable cause, String code, Serializable... params) {
        super(code, cause);
        this.code = code;
        this.params = params;
    }
}
