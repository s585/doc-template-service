package ru.sberbank.sbercrm.saas.doctemplate.document.scheduler;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.sberbank.sbercrm.saas.doctemplate.document.usecase.GenerationJobDispatchUseCase;
import ru.sberbank.sbercrm.saas.doctemplate.template.properties.DocTemplateProperties;

@Component
@RequiredArgsConstructor
public class GenerationJobScheduler {
    private final GenerationJobDispatchUseCase generationJobDispatchUseCase;
    private final DocTemplateProperties docTemplateProperties;

    @Scheduled(fixedDelayString = "${saas.doc-template.generation.dispatch-fixed-delay-ms:250}")
    public void execute() {
        if (!docTemplateProperties.getGeneration().isEnabled()) {
            return;
        }
        generationJobDispatchUseCase.dispatch();
    }
}
