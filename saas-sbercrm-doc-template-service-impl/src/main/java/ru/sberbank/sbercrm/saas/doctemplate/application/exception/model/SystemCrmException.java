package ru.sberbank.sbercrm.saas.doctemplate.application.exception.model;

import java.io.Serializable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class SystemCrmException extends AbstractCrmException {
    public SystemCrmException(String code, String message) {
        super(code, message, new Serializable[0]);
    }

    public SystemCrmException(String code, String message, Serializable... params) {
        super(code, message, params);
    }

    public SystemCrmException(String code, String message, Throwable cause) {
        super(code, message, cause, new Serializable[0]);
    }

    public SystemCrmException(String code, String message, Throwable cause, Serializable... params) {
        super(code, message, cause, params);
    }
}
