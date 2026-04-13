package ru.sberbank.sbercrm.saas.doctemplate.application.exception.model;

import lombok.Getter;
import org.apache.commons.lang3.ArrayUtils;

import java.io.Serializable;

@Getter
public abstract class AbstractCrmException extends RuntimeException {
    private final String code;
    private final Serializable[] params;

    protected AbstractCrmException(String code, String message) {
        this(code, message, ArrayUtils.EMPTY_OBJECT_ARRAY);
    }

    protected AbstractCrmException(String code, String message, Serializable... params) {
        super(message);
        this.code = code;
        this.params = params;
    }

    protected AbstractCrmException(String code, String message, Throwable cause, Serializable... params) {
        super(message, cause);
        this.code = code;
        this.params = params;
    }
}
