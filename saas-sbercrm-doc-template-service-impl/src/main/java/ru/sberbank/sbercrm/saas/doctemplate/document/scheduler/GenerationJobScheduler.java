package ru.sberbank.sbercrm.saas.doctemplate.document.scheduler;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.sberbank.sbercrm.saas.doctemplate.document.usecase.GenerationJobDispatchUseCase;

@Component
@ConditionalOnProperty(
    prefix = "saas.doc-template.generation",
    name = "scheduler-enabled",
    havingValue = "true",
    matchIfMissing = true
)
@RequiredArgsConstructor
public class GenerationJobScheduler {
    private final GenerationJobDispatchUseCase generationJobDispatchUseCase;

    @Scheduled(fixedDelayString = "${saas.doc-template.generation.dispatch-fixed-delay-ms:250}")
    public void execute() {
        generationJobDispatchUseCase.dispatch();
    }
}
