package ru.sberbank.sbercrm.saas.doctemplate.document.service;

import java.util.Set;
import org.springframework.stereotype.Service;
import ru.sberbank.sbercrm.saas.doctemplate.application.exception.CrmErrorCodes;
import ru.sberbank.sbercrm.saas.doctemplate.application.exception.model.AbstractCrmException;
import ru.sberbank.sbercrm.saas.doctemplate.document.constant.DocumentConstants;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.GenerationErrorDecision;

@Service
public class GenerationErrorClassifierImpl implements GenerationErrorClassifier {
    private static final Set<String> RETRIABLE_ERROR_CODES = Set.of(
        CrmErrorCodes.FILE_STORAGE_REQUEST_FAILED,
        DocumentConstants.ErrorCodes.GENERATION_JOB_TIMEOUT
    );

    @Override
    public GenerationErrorDecision classify(Throwable throwable) {
        for (Throwable cause = throwable; cause != null; cause = cause.getCause()) {
            if (cause instanceof AbstractCrmException crmException) {
                return classify(
                    crmException.getCode(),
                    resolveMessage(crmException.getMessage(), crmException.getCode())
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
    public GenerationErrorDecision classify(String errorCode, String errorMessage) {
        String normalizedCode = errorCode == null || errorCode.isBlank()
            ? CrmErrorCodes.SYSTEM_UNEXPECTED
            : errorCode;
        return new GenerationErrorDecision(
            normalizedCode,
            resolveMessage(errorMessage, normalizedCode),
            RETRIABLE_ERROR_CODES.contains(normalizedCode)
        );
    }

    private String resolveMessage(String message, String fallback) {
        return message == null || message.isBlank() ? fallback : message;
    }
}
