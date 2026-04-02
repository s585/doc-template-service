package ru.sberbank.sbercrm.saas.doctemplate.document.scheduler;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.sberbank.sbercrm.saas.doctemplate.document.usecase.GenerationJobDispatchUseCase;
import ru.sberbank.sbercrm.saas.doctemplate.template.properties.TemplateProperties;

@Component
@RequiredArgsConstructor
public class GenerationJobScheduler {
    private final GenerationJobDispatchUseCase generationJobDispatchUseCase;
    private final TemplateProperties templateProperties;

    @Scheduled(fixedDelayString = "${saas.doc-template.generation.dispatch-fixed-delay-ms:250}")
    public void execute() {
        if (!templateProperties.getGeneration().isEnabled()) {
            return;
        }
        generationJobDispatchUseCase.dispatch();
    }
}
