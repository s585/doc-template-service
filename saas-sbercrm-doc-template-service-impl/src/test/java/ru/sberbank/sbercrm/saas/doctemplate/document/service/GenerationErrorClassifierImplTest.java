package ru.sberbank.sbercrm.saas.doctemplate.document.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.sberbank.sbercrm.saas.doctemplate.application.exception.CrmErrorCodes;
import ru.sberbank.sbercrm.saas.doctemplate.document.constant.DocumentConstants;
import ru.sberbank.sbercrm.saas.doctemplate.application.exception.model.SystemCrmException;

class GenerationErrorClassifierImplTest {
    private final GenerationErrorClassifierImpl systemUnderTest = new GenerationErrorClassifierImpl();

    @Test
    @DisplayName("Classifier считает file storage ошибку retriable")
    void givenFileStorageException_whenClassify_thenRetriable() {
        var decision = systemUnderTest.classify(
            new SystemCrmException(CrmErrorCodes.FILE_STORAGE_REQUEST_FAILED, CrmErrorCodes.FILE_STORAGE_REQUEST_FAILED)
        );

        assertThat(decision.errorCode()).isEqualTo(CrmErrorCodes.FILE_STORAGE_REQUEST_FAILED);
        assertThat(decision.retriable()).isTrue();
    }

    @Test
    @DisplayName("Classifier считает core client ошибку retriable")
    void givenCoreClientException_whenClassify_thenRetriable() {
        var decision = systemUnderTest.classify(
            new SystemCrmException(CrmErrorCodes.CORE_CLIENT_REQUEST_FAILED, CrmErrorCodes.CORE_CLIENT_REQUEST_FAILED)
        );

        assertThat(decision.errorCode()).isEqualTo(CrmErrorCodes.CORE_CLIENT_REQUEST_FAILED);
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
                new SystemCrmException(CrmErrorCodes.FILE_STORAGE_REQUEST_FAILED, CrmErrorCodes.FILE_STORAGE_REQUEST_FAILED)
            )
        );

        assertThat(decision.errorCode()).isEqualTo(CrmErrorCodes.FILE_STORAGE_REQUEST_FAILED);
        assertThat(decision.retriable()).isTrue();
    }
}
