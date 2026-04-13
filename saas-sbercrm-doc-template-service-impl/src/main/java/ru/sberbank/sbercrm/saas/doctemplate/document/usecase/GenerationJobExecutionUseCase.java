package ru.sberbank.sbercrm.saas.doctemplate.document.usecase;

import ru.sberbank.sbercrm.saas.doctemplate.document.model.GenerationJob;

/**
 * Исполнение одной {@link GenerationJob}.
 *
 * <p>Работает после claim и отвечает за lifecycle конкретной job: создание attempt, генерацию
 * результата и выбор перехода retry, fail или complete.
 */
public interface GenerationJobExecutionUseCase {
    void execute(GenerationJob job);
}
