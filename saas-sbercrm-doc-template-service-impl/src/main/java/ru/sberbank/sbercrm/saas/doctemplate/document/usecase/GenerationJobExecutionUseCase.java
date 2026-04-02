package ru.sberbank.sbercrm.saas.doctemplate.document.usecase;

import ru.sberbank.sbercrm.saas.doctemplate.document.model.GenerationJob;

public interface GenerationJobExecutionUseCase {
    void execute(GenerationJob job);
}
