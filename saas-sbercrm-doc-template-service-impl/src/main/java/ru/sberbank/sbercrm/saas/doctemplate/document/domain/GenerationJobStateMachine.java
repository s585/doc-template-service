package ru.sberbank.sbercrm.saas.doctemplate.document.domain;

import org.checkerframework.checker.nullness.qual.Nullable;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.GenerationJobEvent;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.GenerationJobStatus;

/**
 * Локальная state machine для {@code generation_job}.
 *
 * <p>Используется как единственная точка правил перехода между состояниями job.
 */
public interface GenerationJobStateMachine {
    /**
     * Вычисляет новое состояние job по текущему статусу и событию.
     *
     * <p>Недопустимый переход считается ошибкой реализации.
     */
    GenerationJobStatus transit(@Nullable GenerationJobStatus currentStatus, @Nullable GenerationJobEvent event);
}
