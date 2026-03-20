package ru.sberbank.sbercrm.saas.doctemplate.application.exception.model;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.io.Serializable;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class BusinessCrmException extends AbstractCrmException {
    public BusinessCrmException(String code, Serializable... params) {
        super(code, params);
    }

    public BusinessCrmException(Throwable cause, String code, Serializable... params) {
        super(cause, code, params);
    }
}
