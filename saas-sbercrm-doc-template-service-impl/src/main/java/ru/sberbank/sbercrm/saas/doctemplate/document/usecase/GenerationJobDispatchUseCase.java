package ru.sberbank.sbercrm.saas.doctemplate.document.usecase;

/**
 * Dispatch-слой обработки generation job.
 *
 * <p>Scheduler вызывает этот use case часто. Dispatch делает recovery timed out
 * job, claim-ит доступные записи и передаёт их в асинхронное исполнение.
 */
public interface GenerationJobDispatchUseCase {
    void dispatch();
}
