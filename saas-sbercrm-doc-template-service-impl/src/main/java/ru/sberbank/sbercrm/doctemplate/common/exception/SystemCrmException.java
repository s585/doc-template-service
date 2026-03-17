package ru.sberbank.sbercrm.doctemplate.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.io.Serializable;

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class SystemCrmException extends AbstractCrmException {
    public SystemCrmException(String code, Serializable... params) {
        super(code, params);
    }

    public SystemCrmException(Throwable cause, String code, Serializable... params) {
        super(cause, code, params);
    }
}
