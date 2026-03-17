package ru.sberbank.sbercrm.doctemplate.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.io.Serializable;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class NotFoundCrmException extends BusinessCrmException {
    public NotFoundCrmException(String code, Serializable... params) {
        super(code, params);
    }

    public NotFoundCrmException(Throwable cause, String code, Serializable... params) {
        super(cause, code, params);
    }
}
