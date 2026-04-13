package ru.sberbank.sbercrm.saas.doctemplate.document.service;

import java.util.UUID;

/**
 * Источник runtime identity worker-а, который dispatch-ит и исполняет generation job.
 *
 * <p>{@code workerId} используется для связи job и attempt в БД, а
 * {@code workerName} и {@code executionName} — для корреляции с логами.
 */
public interface GenerationWorkerIdentityProvider {
    UUID getWorkerId();

    String getWorkerName();

    default String getExecutionName() {
        return getWorkerName() + ":" + Thread.currentThread().getName();
    }
}
