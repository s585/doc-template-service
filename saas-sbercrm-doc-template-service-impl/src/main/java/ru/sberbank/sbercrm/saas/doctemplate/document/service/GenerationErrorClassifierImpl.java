package ru.sberbank.sbercrm.saas.doctemplate.document.service;

import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import ru.sberbank.sbercrm.saas.doctemplate.application.exception.CrmErrorCodes;
import ru.sberbank.sbercrm.saas.doctemplate.application.exception.model.AbstractCrmException;
import ru.sberbank.sbercrm.saas.doctemplate.document.constant.DocumentConstants;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.GenerationErrorDecision;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GenerationErrorClassifierImpl implements GenerationErrorClassifier {
    private static final Set<String> RETRIABLE_ERROR_CODES = Set.of(
        CrmErrorCodes.FILE_STORAGE_REQUEST_FAILED,
        CrmErrorCodes.CORE_CLIENT_REQUEST_FAILED,
        DocumentConstants.ErrorCodes.GENERATION_JOB_TIMEOUT
    );

    private final MessageSource messageSource;

    @Override
    public GenerationErrorDecision classify(@Nullable Throwable throwable) {
        for (Throwable cause = throwable; cause != null; cause = cause.getCause()) {
            if (cause instanceof AbstractCrmException crmException) {
                return classify(
                    crmException.getCode(),
                    resolveMessage(crmException)
                );
            }
        }
        return classify(
            CrmErrorCodes.SYSTEM_UNEXPECTED,
            resolveMessage(
                throwable == null ? null : throwable.getMessage(),
                CrmErrorCodes.SYSTEM_UNEXPECTED
            )
        );
    }

    @Override
    public GenerationErrorDecision classify(@Nullable String errorCode, @Nullable String errorMessage) {
        String normalizedCode = errorCode == null || errorCode.isBlank()
            ? CrmErrorCodes.SYSTEM_UNEXPECTED
            : errorCode;
        return new GenerationErrorDecision(
            normalizedCode,
            resolveMessage(errorMessage, normalizedCode),
            RETRIABLE_ERROR_CODES.contains(normalizedCode)
        );
    }

    private String resolveMessage(@Nullable String message, String fallback) {
        return message == null || message.isBlank() ? fallback : message;
    }

    private String resolveMessage(AbstractCrmException exception) {
        Object[] params = exception.getParams() == null ? new Object[0] : exception.getParams();
        return messageSource.getMessage(
            exception.getCode(),
            params,
            exception.getCode(),
            LocaleContextHolder.getLocale()
        );
    }
}
