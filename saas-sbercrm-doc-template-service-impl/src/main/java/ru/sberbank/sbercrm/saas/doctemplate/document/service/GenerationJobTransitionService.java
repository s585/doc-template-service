package ru.sberbank.sbercrm.saas.doctemplate.document.service;

import java.util.List;
import java.util.UUID;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.GenerationArtifactMeta;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.GeneratedFileResult;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.GenerationJob;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.GenerationJobAttempt;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.GenerationJobPreAttemptContext;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.GenerationRetryDecision;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.GenerationTransitionContext;

/**
 * Транзакционная граница переходов generation flow.
 *
 * <p>Через этот сервис синхронно обновляются согласованные состояния
 * {@code generated_file}, {@code generation_job} и {@code generation_job_attempt}.
 */
public interface GenerationJobTransitionService {
    /**
     * Claim-ит доступные job для выполнения через явный transition {@code QUEUED -> PROCESSING}.
     */
    List<GenerationJob> claimNextJobsForProcessing(UUID workerId, int limit);

    /**
     * Создаёт новую attempt и переводит связанный {@code generated_file} в {@code PROCESSING}.
     *
     * <p>Этот шаг должен быть атомарным, чтобы active attempt и статус файла не расходились между собой.
     */
    GenerationJobAttempt startGenerationAttempt(GenerationJob job, UUID userId);

    /**
     * Выполняет recovery timed out job и возвращает количество реально обработанных записей.
     */
    int requeueTimedOutJobs(UUID userId);

    /**
     * Планирует повторное выполнение job, если execution оборвался до создания attempt.
     */
    void scheduleRetryBeforeAttempt(
        GenerationJobPreAttemptContext context,
        GenerationRetryDecision retryDecision
    );

    /**
     * Фиксирует финальную ошибку job, если execution оборвался до создания attempt.
     */
    void failBeforeAttempt(
        GenerationJobPreAttemptContext context,
        GenerationRetryDecision retryDecision
    );

    /**
     * Планирует повторное выполнение job после retriable ошибки.
     *
     * <p>Переход выполняется только для актуальной активной попытки, чтобы поздний старый worker не смог
     * перетереть более новое состояние job.
     */
    void retryGeneration(GenerationTransitionContext context, GenerationRetryDecision retryDecision);

    /**
     * Фиксирует факт загрузки артефакта в хранилище до финального COMPLETE перехода.
     *
     * <p>Вызывается в отдельной транзакции, чтобы ключ можно было переиспользовать при retry,
     * даже если финальный COMPLETE не закоммитился.
     */
    void persistUploadedArtifact(GenerationTransitionContext context, GenerationArtifactMeta artifactMeta);

    /**
     * Фиксирует успешное завершение generation job и связанного generated file.
     *
     * <p>Переход защищён optimistic guard-ом по номеру попытки.
     */
    void completeGeneration(GenerationTransitionContext context, GeneratedFileResult generatedFileResult);

    /**
     * Фиксирует финальную ошибку generation job без повторного запуска.
     *
     * <p>Переход защищён optimistic guard-ом по номеру попытки.
     */
    void failGeneration(GenerationTransitionContext context, GenerationRetryDecision retryDecision);
}
