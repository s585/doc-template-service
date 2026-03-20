package ru.sberbank.sbercrm.saas.doctemplate.application.exception;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.sberbank.sbercrm.saas.doctemplate.application.exception.model.AbstractCrmException;
import ru.sberbank.sbercrm.saas.doctemplate.application.exception.model.BusinessCrmException;
import ru.sberbank.sbercrm.saas.doctemplate.application.exception.model.NotFoundCrmException;
import ru.sberbank.sbercrm.saas.doctemplate.application.exception.model.SystemCrmException;

import java.io.Serializable;
import java.util.Map;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class CrmExceptionHandler {
    private final MessageSource messageSource;

    @ExceptionHandler(NotFoundCrmException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(NotFoundCrmException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorBody(ex));
    }

    @ExceptionHandler(BusinessCrmException.class)
    public ResponseEntity<Map<String, Object>> handleBusiness(BusinessCrmException ex) {
        return ResponseEntity.badRequest().body(errorBody(ex));
    }

    @ExceptionHandler(SystemCrmException.class)
    public ResponseEntity<Map<String, Object>> handleSystem(SystemCrmException ex) {
        log.error("System CRM exception", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorBody(ex));
    }

    private Map<String, Object> errorBody(AbstractCrmException ex) {
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
