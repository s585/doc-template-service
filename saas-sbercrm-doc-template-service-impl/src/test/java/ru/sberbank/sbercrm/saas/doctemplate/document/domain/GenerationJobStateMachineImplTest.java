package ru.sberbank.sbercrm.saas.doctemplate.document.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.sberbank.sbercrm.saas.doctemplate.application.exception.model.SystemCrmException;
import ru.sberbank.sbercrm.saas.doctemplate.document.constant.DocumentConstants;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.GenerationJobEvent;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.GenerationJobStatus;

class GenerationJobStateMachineImplTest {
    private final GenerationJobStateMachineImpl systemUnderTest = new GenerationJobStateMachineImpl();

    @Test
    @DisplayName("State machine разрешает переход PROCESSING -> DONE по событию COMPLETE")
    void givenProcessingStatus_whenComplete_thenReturnDone() {
        assertThat(systemUnderTest.transit(GenerationJobStatus.PROCESSING, GenerationJobEvent.COMPLETE))
            .isEqualTo(GenerationJobStatus.DONE);
    }

    @Test
    @DisplayName("State machine разрешает переход PROCESSING -> QUEUED по событию TIMEOUT")
    void givenProcessingStatus_whenTimeout_thenReturnQueued() {
        assertThat(systemUnderTest.transit(GenerationJobStatus.PROCESSING, GenerationJobEvent.TIMEOUT))
            .isEqualTo(GenerationJobStatus.QUEUED);
    }

    @Test
    @DisplayName("State machine разрешает переход PROCESSING -> QUEUED по событию RETRY")
    void givenProcessingStatus_whenRetry_thenReturnQueued() {
        assertThat(systemUnderTest.transit(GenerationJobStatus.PROCESSING, GenerationJobEvent.RETRY))
            .isEqualTo(GenerationJobStatus.QUEUED);
    }

    @Test
    @DisplayName("State machine запрещает недопустимый переход")
    void givenQueuedStatus_whenComplete_thenThrow() {
        assertThatThrownBy(() -> systemUnderTest.transit(GenerationJobStatus.QUEUED, GenerationJobEvent.COMPLETE))
            .isInstanceOf(SystemCrmException.class)
            .extracting("code")
            .isEqualTo(DocumentConstants.ErrorCodes.GENERATION_JOB_TRANSITION_INVALID);
    }

    @Test
    @DisplayName("State machine запрещает null-аргументы")
    void givenNullArgument_whenTransit_thenThrowCrmException() {
        assertThatThrownBy(() -> systemUnderTest.transit(null, GenerationJobEvent.COMPLETE))
            .isInstanceOf(SystemCrmException.class)
            .extracting("code")
            .isEqualTo(DocumentConstants.ErrorCodes.GENERATION_JOB_TRANSITION_INVALID);
    }
}
