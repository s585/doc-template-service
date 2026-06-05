package ru.sberbank.sbercrm.saas.doctemplate.document.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticMessageSource;
import ru.sberbank.sbercrm.saas.doctemplate.application.exception.CrmErrorCodes;
import ru.sberbank.sbercrm.saas.doctemplate.application.exception.model.BusinessCrmException;
import ru.sberbank.sbercrm.saas.doctemplate.document.constant.DocumentConstants;
import ru.sberbank.sbercrm.saas.doctemplate.application.exception.model.SystemCrmException;

class GenerationErrorClassifierImplTest {
    private final StaticMessageSource messageSource = new StaticMessageSource();
    private final GenerationErrorClassifierImpl systemUnderTest = new GenerationErrorClassifierImpl(messageSource);

    GenerationErrorClassifierImplTest() {
        messageSource.addMessage(CrmErrorCodes.FILE_STORAGE_REQUEST_FAILED, java.util.Locale.getDefault(), "File storage failed: {0}");
        messageSource.addMessage(CrmErrorCodes.CORE_CLIENT_REQUEST_FAILED, java.util.Locale.getDefault(), "Core client failed: {0}");
        messageSource.addMessage(DocumentConstants.ErrorCodes.GENERATION_BUSINESS_OBJECT_PATH_INVALID, java.util.Locale.getDefault(),
            "Invalid business object path for mapping: key={0}, path={1}");
    }

    @Test
    @DisplayName("Classifier считает file storage ошибку retriable")
    void givenFileStorageException_whenClassify_thenRetriable() {
        var decision = systemUnderTest.classify(
            new SystemCrmException(
                CrmErrorCodes.FILE_STORAGE_REQUEST_FAILED,
                CrmErrorCodes.FILE_STORAGE_REQUEST_FAILED,
                "contract.docx"
            )
        );

        assertThat(decision.errorCode()).isEqualTo(CrmErrorCodes.FILE_STORAGE_REQUEST_FAILED);
        assertThat(decision.errorMessage()).isEqualTo("File storage failed: contract.docx");
        assertThat(decision.retriable()).isTrue();
    }

    @Test
    @DisplayName("Classifier считает core client ошибку retriable")
    void givenCoreClientException_whenClassify_thenRetriable() {
        var decision = systemUnderTest.classify(
            new SystemCrmException(
                CrmErrorCodes.CORE_CLIENT_REQUEST_FAILED,
                CrmErrorCodes.CORE_CLIENT_REQUEST_FAILED,
                "status=500"
            )
        );

        assertThat(decision.errorCode()).isEqualTo(CrmErrorCodes.CORE_CLIENT_REQUEST_FAILED);
        assertThat(decision.errorMessage()).isEqualTo("Core client failed: status=500");
        assertThat(decision.retriable()).isTrue();
    }

    @Test
    @DisplayName("Classifier считает обычную runtime ошибку non-retriable")
    void givenRuntimeException_whenClassify_thenNonRetriable() {
        var decision = systemUnderTest.classify(new IllegalStateException("boom"));

        assertThat(decision.errorCode()).isEqualTo(CrmErrorCodes.SYSTEM_UNEXPECTED);
        assertThat(decision.retriable()).isFalse();
    }

    @Test
    @DisplayName("Classifier считает timeout код retriable")
    void givenTimeoutCode_whenClassify_thenRetriable() {
        var decision = systemUnderTest.classify(
            DocumentConstants.ErrorCodes.GENERATION_JOB_TIMEOUT,
            "Generation job timed out"
        );

        assertThat(decision.errorCode()).isEqualTo(DocumentConstants.ErrorCodes.GENERATION_JOB_TIMEOUT);
        assertThat(decision.errorMessage()).isEqualTo("Generation job timed out");
        assertThat(decision.retriable()).isTrue();
    }

    @Test
    @DisplayName("Classifier поднимается по cause chain до AbstractCrmException")
    void givenNestedCrmException_whenClassify_thenUseNestedCode() {
        var decision = systemUnderTest.classify(
            new RuntimeException(
                "wrapper",
                new SystemCrmException(
                    CrmErrorCodes.FILE_STORAGE_REQUEST_FAILED,
                    CrmErrorCodes.FILE_STORAGE_REQUEST_FAILED,
                    "generated.docx"
                )
            )
        );

        assertThat(decision.errorCode()).isEqualTo(CrmErrorCodes.FILE_STORAGE_REQUEST_FAILED);
        assertThat(decision.errorMessage()).isEqualTo("File storage failed: generated.docx");
        assertThat(decision.retriable()).isTrue();
    }

    @Test
    @DisplayName("Classifier резолвит message code с параметрами для бизнес-ошибки генерации")
    void givenBusinessCrmExceptionWithParams_whenClassify_thenResolveLocalizedMessage() {
        var decision = systemUnderTest.classify(
            new BusinessCrmException(
                DocumentConstants.ErrorCodes.GENERATION_BUSINESS_OBJECT_PATH_INVALID,
                DocumentConstants.ErrorCodes.GENERATION_BUSINESS_OBJECT_PATH_INVALID,
                "Resp",
                "source.owner.name"
            )
        );

        assertThat(decision.errorCode()).isEqualTo(DocumentConstants.ErrorCodes.GENERATION_BUSINESS_OBJECT_PATH_INVALID);
        assertThat(decision.errorMessage()).isEqualTo(
            "Invalid business object path for mapping: key=Resp, path=source.owner.name"
        );
        assertThat(decision.retriable()).isFalse();
    }
}
