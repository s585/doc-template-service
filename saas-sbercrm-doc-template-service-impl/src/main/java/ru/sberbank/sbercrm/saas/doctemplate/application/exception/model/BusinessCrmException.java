package ru.sberbank.sbercrm.saas.doctemplate.application.exception.model;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class BusinessCrmException extends AbstractCrmException {
    public BusinessCrmException(String code, Object... params) {
        super(code, params);
    }

    public BusinessCrmException(Throwable cause, String code, Object... params) {
        super(cause, code, params);
    }
}
