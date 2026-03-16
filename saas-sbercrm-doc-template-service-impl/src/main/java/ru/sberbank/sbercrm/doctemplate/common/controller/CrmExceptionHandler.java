package ru.sberbank.sbercrm.doctemplate.common.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.sberbank.sbercrm.doctemplate.common.exception.BusinessCrmException;
import ru.sberbank.sbercrm.doctemplate.common.exception.SystemCrmException;

import java.io.Serializable;
import java.util.Map;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class CrmExceptionHandler {
    private final MessageSource messageSource;

    @ExceptionHandler(BusinessCrmException.class)
    public ResponseEntity<Map<String, Object>> handleBusiness(BusinessCrmException ex) {
        return ResponseEntity.badRequest().body(errorBody(ex));
    }

    @ExceptionHandler(SystemCrmException.class)
    public ResponseEntity<Map<String, Object>> handleSystem(SystemCrmException ex) {
        log.error("System CRM exception", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorBody(ex));
    }

    private Map<String, Object> errorBody(ru.sberbank.sbercrm.doctemplate.common.exception.AbstractCrmException ex) {
        Serializable[] params = ex.getParams() == null ? new Serializable[0] : ex.getParams();
        return Map.of(
            "code", ex.getCode(),
            "message", resolveMessage(ex.getCode(), params),
            "params", params
        );
    }

    private String resolveMessage(String code, Object[] params) {
        return messageSource.getMessage(
            code,
            params,
            code,
            LocaleContextHolder.getLocale()
        );
    }
}
