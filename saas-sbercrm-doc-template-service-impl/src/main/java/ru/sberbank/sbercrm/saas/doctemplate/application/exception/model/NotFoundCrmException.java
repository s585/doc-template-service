package ru.sberbank.sbercrm.saas.doctemplate.application.exception.model;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class NotFoundCrmException extends BusinessCrmException {
    public NotFoundCrmException(String code, Object... params) {
        super(code, params);
    }

    public NotFoundCrmException(Throwable cause, String code, Object... params) {
        super(cause, code, params);
    }
}
