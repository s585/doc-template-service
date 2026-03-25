package ru.sberbank.sbercrm.saas.doctemplate.application.exception.model;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class SystemCrmException extends AbstractCrmException {
    public SystemCrmException(String code, Object... params) {
        super(code, params);
    }

    public SystemCrmException(Throwable cause, String code, Object... params) {
        super(cause, code, params);
    }
}
