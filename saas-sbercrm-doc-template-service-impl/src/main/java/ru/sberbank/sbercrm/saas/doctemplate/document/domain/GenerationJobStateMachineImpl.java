package ru.sberbank.sbercrm.saas.doctemplate.document.domain;

import java.util.Map;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.stereotype.Component;
import ru.sberbank.sbercrm.saas.doctemplate.application.exception.model.SystemCrmException;
import ru.sberbank.sbercrm.saas.doctemplate.document.constant.DocumentConstants;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.GenerationJobEvent;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.GenerationJobStatus;

@Component
public class GenerationJobStateMachineImpl implements GenerationJobStateMachine {
    private static final Map<GenerationJobStatus, Map<GenerationJobEvent, GenerationJobStatus>> TRANSITIONS = Map.of(
        GenerationJobStatus.QUEUED, Map.of(
            GenerationJobEvent.CLAIM, GenerationJobStatus.PROCESSING
        ),
        GenerationJobStatus.PROCESSING, Map.of(
            GenerationJobEvent.TIMEOUT, GenerationJobStatus.QUEUED,
            GenerationJobEvent.RETRY, GenerationJobStatus.QUEUED,
            GenerationJobEvent.COMPLETE, GenerationJobStatus.DONE,
            GenerationJobEvent.FAIL, GenerationJobStatus.ERROR
        )
    );

    @Override
    public GenerationJobStatus transit(@Nullable GenerationJobStatus currentStatus, @Nullable GenerationJobEvent event) {
        if (currentStatus == null || event == null) {
            throw new SystemCrmException(
                DocumentConstants.ErrorCodes.GENERATION_JOB_TRANSITION_INVALID,
                DocumentConstants.ErrorCodes.GENERATION_JOB_TRANSITION_INVALID,
                String.valueOf(currentStatus),
                String.valueOf(event)
            );
        }

        GenerationJobStatus nextStatus = TRANSITIONS.getOrDefault(currentStatus, Map.of()).get(event);
        if (nextStatus == null) {
            throw new SystemCrmException(
                DocumentConstants.ErrorCodes.GENERATION_JOB_TRANSITION_INVALID,
                DocumentConstants.ErrorCodes.GENERATION_JOB_TRANSITION_INVALID,
                currentStatus.name(),
                event.name()
            );
        }
        return nextStatus;
    }
}
