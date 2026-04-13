package ru.sberbank.sbercrm.saas.doctemplate.application.exception.model;

import java.io.Serializable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class NotFoundCrmException extends BusinessCrmException {
    public NotFoundCrmException(String code, String message) {
        super(code, message, new Serializable[0]);
    }

    public NotFoundCrmException(String code, String message, Serializable... params) {
        super(code, message, params);
    }

    public NotFoundCrmException(String code, String message, Throwable cause) {
        super(code, message, cause, new Serializable[0]);
    }

    public NotFoundCrmException(String code, String message, Throwable cause, Serializable... params) {
        super(code, message, cause, params);
    }
}
