package ru.sberbank.sbercrm.saas.doctemplate.document.service;

import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import ru.sberbank.sbercrm.saas.doctemplate.application.exception.model.NotFoundCrmException;
import ru.sberbank.sbercrm.saas.doctemplate.document.constant.DocumentConstants;
import ru.sberbank.sbercrm.saas.doctemplate.document.domain.GenerationJobStateMachine;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.GeneratedFileResult;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.GenerationJob;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.GenerationJobAttempt;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.GenerationJobEvent;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.GenerationJobPreAttemptContext;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.GenerationJobRetryCmd;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.GenerationJobStatus;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.GenerationRetryDecision;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.GenerationTransitionContext;

@Slf4j
@Service
public class GenerationJobTransitionServiceImpl implements GenerationJobTransitionService {
    private final GenerationJobService generationJobService;
    private final GeneratedFileService generatedFileService;
    private final GenerationJobAttemptService generationJobAttemptService;
    private final GenerationJobStateMachine generationJobStateMachine;
    private final GenerationErrorClassifier generationErrorClassifier;
    private final GenerationRetryPolicy generationRetryPolicy;
    private final GenerationWorkerIdentityProvider generationWorkerIdentityProvider;
    private final TransactionTemplate recoveryTransactionTemplate;

    private static final String GENERATION_JOB_TIMEOUT_MESSAGE = "Generation job timed out";

    public GenerationJobTransitionServiceImpl(
        GenerationJobService generationJobService,
        GeneratedFileService generatedFileService,
        GenerationJobAttemptService generationJobAttemptService,
        GenerationJobStateMachine generationJobStateMachine,
        GenerationErrorClassifier generationErrorClassifier,
        GenerationRetryPolicy generationRetryPolicy,
        GenerationWorkerIdentityProvider generationWorkerIdentityProvider,
        PlatformTransactionManager transactionManager
    ) {
        this.generationJobService = generationJobService;
        this.generatedFileService = generatedFileService;
        this.generationJobAttemptService = generationJobAttemptService;
        this.generationJobStateMachine = generationJobStateMachine;
        this.generationErrorClassifier = generationErrorClassifier;
        this.generationRetryPolicy = generationRetryPolicy;
        this.generationWorkerIdentityProvider = generationWorkerIdentityProvider;
        this.recoveryTransactionTemplate = new TransactionTemplate(transactionManager);
        this.recoveryTransactionTemplate.setPropagationBehavior(
            org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW
        );
    }

    @Override
    @Transactional
    public GenerationJobAttempt startGenerationAttempt(GenerationJob job, UUID userId) {
        GenerationJobAttempt attempt = generationJobAttemptService.create(userId, job.getId(), job.getLockedBy());
        generatedFileService.markProcessing(
                job.getTenantId(),
                userId,
                job.getDocumentId(),
                job.getFormat()
        );
        return attempt;
    }

    @Override
    public int requeueTimedOutJobs(UUID userId) {
        int requeuedJobs = 0;

        for (GenerationJob job : generationJobService.findTimedOutJobs()) {
            if (requeueTimedOutJobSafely(job, userId)) {
                requeuedJobs++;
            }
        }

        return requeuedJobs;
    }

    @Override
    @Transactional
    public void scheduleRetryBeforeAttempt(
        GenerationJobPreAttemptContext context,
        GenerationRetryDecision retryDecision
    ) {
        int nextAttemptNo = context.currentAttemptCount() + 1;
        if (!generationJobService.scheduleRetry(
                buildRetryCmd(
                    context.tenantId(),
                    context.userId(),
                    context.jobId(),
                    context.currentAttemptCount(),
                    nextAttemptNo,
                    retryDecision
                ))) {
            logStalePreAttemptTransition(context, nextAttemptNo, "init-failure-retry");
            return;
        }
        generatedFileService.markPending(
                context.tenantId(),
                context.userId(),
                context.documentId(),
                context.format()
        );
    }

    @Override
    @Transactional
    public void failBeforeAttempt(
        GenerationJobPreAttemptContext context,
        GenerationRetryDecision retryDecision
    ) {
        int nextAttemptNo = context.currentAttemptCount() + 1;
        if (!generationJobService.markFailed(
                context.tenantId(),
                context.userId(),
                context.jobId(),
                context.currentAttemptCount(),
                nextAttemptNo,
                retryDecision.errorCode(),
                retryDecision.errorMessage())) {
            logStalePreAttemptTransition(context, nextAttemptNo, "init-failure-fail");
            return;
        }
        generatedFileService.markFailed(
                context.tenantId(),
                context.userId(),
                context.documentId(),
                context.format(),
                retryDecision.errorCode(),
                retryDecision.errorMessage()
        );
    }

    @Override
    @Transactional
    public void retryGeneration(GenerationTransitionContext context, GenerationRetryDecision retryDecision) {
        GenerationJob job = getRequiredJob(context.tenantId(), context.jobId());
        generationJobStateMachine.transit(
                GenerationJobStatus.fromValue(job.getStatus()),
                GenerationJobEvent.RETRY
        );

        if (!generationJobService.scheduleRetry(
                buildRetryCmd(
                    context.tenantId(),
                    context.userId(),
                    context.jobId(),
                    expectedAttemptCount(context.attemptNo()),
                    context.attemptNo(),
                    retryDecision
                ))) {
            logStaleTransition(job, context.attemptNo(), "retry");
            return;
        }

        generatedFileService.markPending(
                context.tenantId(),
                context.userId(),
                context.documentId(),
                context.format()
        );
        generationJobAttemptService.markFailed(
                context.userId(),
                context.attemptId(),
                retryDecision.errorCode(),
                retryDecision.errorMessage()
        );
    }

    @Override
    @Transactional
    public void completeGeneration(GenerationTransitionContext context, GeneratedFileResult generatedFileResult) {
        GenerationJob job = getRequiredJob(context.tenantId(), context.jobId());
        generationJobStateMachine.transit(
                GenerationJobStatus.fromValue(job.getStatus()),
                GenerationJobEvent.COMPLETE
        );

        if (!generationJobService.markCompleted(
                context.tenantId(),
                context.userId(),
                context.jobId(),
                expectedAttemptCount(context.attemptNo()),
                context.attemptNo())) {
            logStaleTransition(job, context.attemptNo(), "complete");
            return;
        }

        generatedFileService.markCompleted(
                context.tenantId(),
                context.userId(),
                context.documentId(),
                context.format(),
                generatedFileResult.s3Key(),
                generatedFileResult.checksum(),
                generatedFileResult.sizeBytes()
        );
        generationJobAttemptService.markCompleted(context.userId(), context.attemptId());
    }

    @Override
    @Transactional
    public void failGeneration(GenerationTransitionContext context, GenerationRetryDecision retryDecision) {
        GenerationJob job = getRequiredJob(context.tenantId(), context.jobId());
        generationJobStateMachine.transit(
                GenerationJobStatus.fromValue(job.getStatus()),
                GenerationJobEvent.FAIL
        );

        if (!generationJobService.markFailed(
                context.tenantId(),
                context.userId(),
                context.jobId(),
                expectedAttemptCount(context.attemptNo()),
                context.attemptNo(),
                retryDecision.errorCode(),
                retryDecision.errorMessage())) {
            logStaleTransition(job, context.attemptNo(), "fail");
            return;
        }

        generatedFileService.markFailed(
                context.tenantId(),
                context.userId(),
                context.documentId(),
                context.format(),
                retryDecision.errorCode(),
                retryDecision.errorMessage()
        );
        generationJobAttemptService.markFailed(
                context.userId(),
                context.attemptId(),
                retryDecision.errorCode(),
                retryDecision.errorMessage()
        );
    }

    private GenerationJob getRequiredJob(UUID tenantId, UUID jobId) {
        return generationJobService
                .findById(tenantId, jobId)
                .orElseThrow(() -> new NotFoundCrmException(
                        DocumentConstants.ErrorCodes.GENERATION_JOB_NOT_FOUND,
                        DocumentConstants.ErrorCodes.GENERATION_JOB_NOT_FOUND,
                        jobId
                ));
    }

    private void requeueTimedOutJob(GenerationJob job, UUID userId) {
        int currentAttemptNo = job.getAttemptCount() + 1;
        GenerationRetryDecision retryDecision = buildTimeoutRetryDecision(currentAttemptNo);

        if (shouldRetry(retryDecision)) {
            scheduleTimedOutJobRetry(job, userId, currentAttemptNo, retryDecision);
            logTimedOutJobRetry(job, currentAttemptNo, retryDecision);
            return;
        }

        failTimedOutJob(job, userId, currentAttemptNo, retryDecision);
        logTimedOutJobFailure(job, currentAttemptNo);
    }

    private boolean requeueTimedOutJobSafely(GenerationJob job, UUID userId) {
        try {
            recoveryTransactionTemplate.executeWithoutResult(
                status -> requeueTimedOutJob(job, userId)
            );
            return true;
        } catch (Exception ex) {
            log.error(
                "Failed to recover timed out generation job: jobId={}, documentId={}, format={}, "
                    + "workerId={}",
                job.getId(),
                job.getDocumentId(),
                job.getFormat(),
                job.getLockedBy(),
                ex
            );
            return false;
        }
    }

    private GenerationRetryDecision buildTimeoutRetryDecision(int currentAttemptNo) {
        return generationRetryPolicy.decide(
                currentAttemptNo,
                generationErrorClassifier.classify(
                        DocumentConstants.ErrorCodes.GENERATION_JOB_TIMEOUT,
                        GENERATION_JOB_TIMEOUT_MESSAGE
                )
        );
    }

    private boolean shouldRetry(GenerationRetryDecision retryDecision) {
        return retryDecision.action().isRetry();
    }

    private GenerationJobRetryCmd buildRetryCmd(
            UUID tenantId,
            UUID userId,
            UUID jobId,
            int expectedAttemptCount,
            int attemptCount,
            GenerationRetryDecision retryDecision
    ) {
        return GenerationJobRetryCmd.builder()
                .tenantId(tenantId)
                .userId(userId)
                .jobId(jobId)
                .expectedAttemptCount(expectedAttemptCount)
                .attemptCount(attemptCount)
                .nextRetryAt(retryDecision.nextRetryAt())
                .errorCode(retryDecision.errorCode())
                .errorMessage(retryDecision.errorMessage())
                .build();
    }

    private void scheduleTimedOutJobRetry(
            GenerationJob job,
            UUID userId,
            int currentAttemptNo,
            GenerationRetryDecision retryDecision
    ) {
        generationJobStateMachine.transit(
                GenerationJobStatus.fromValue(job.getStatus()),
                GenerationJobEvent.TIMEOUT
        );
        if (!generationJobService.scheduleRetry(
                buildRetryCmd(
                    job.getTenantId(),
                    userId,
                    job.getId(),
                    job.getAttemptCount(),
                    currentAttemptNo,
                    retryDecision
                ))) {
            logStaleTransition(job, currentAttemptNo, "timeout-retry");
            return;
        }
        generationJobAttemptService.markTimedOutActiveAttempt(userId, job.getId());
        generatedFileService.markPending(
                job.getTenantId(),
                userId,
                job.getDocumentId(),
                job.getFormat()
        );
    }

    private void failTimedOutJob(
            GenerationJob job,
            UUID userId,
            int currentAttemptNo,
            GenerationRetryDecision retryDecision
    ) {
        generationJobStateMachine.transit(
                GenerationJobStatus.fromValue(job.getStatus()),
                GenerationJobEvent.FAIL
        );
        if (!generationJobService.markFailed(
                job.getTenantId(),
                userId,
                job.getId(),
                job.getAttemptCount(),
                currentAttemptNo,
                retryDecision.errorCode(),
                retryDecision.errorMessage())) {
            logStaleTransition(job, currentAttemptNo, "timeout-fail");
            return;
        }
        generationJobAttemptService.markTimedOutActiveAttempt(userId, job.getId());
        generatedFileService.markFailed(
                job.getTenantId(),
                userId,
                job.getDocumentId(),
                job.getFormat(),
                retryDecision.errorCode(),
                retryDecision.errorMessage()
        );
    }

    private int expectedAttemptCount(int attemptNo) {
        return Math.max(attemptNo - 1, 0);
    }

    private void logStaleTransition(GenerationJob job, int attemptNo, String action) {
        log.warn(
            "Skipped stale generation job transition: action={}, jobId={}, documentId={}, "
                        + "format={}, attemptNo={}, workerId={}",
                action,
                job.getId(),
                job.getDocumentId(),
                job.getFormat(),
                attemptNo,
            job.getLockedBy()
        );
    }

    private void logStalePreAttemptTransition(
        GenerationJobPreAttemptContext context,
        int attemptNo,
        String action
    ) {
        log.warn(
            "Skipped stale pre-attempt generation job transition: action={}, jobId={}, "
                + "documentId={}, format={}, attemptNo={}",
            action,
            context.jobId(),
            context.documentId(),
            context.format(),
            attemptNo
        );
    }

    private void logTimedOutJobRetry(
            GenerationJob job,
            int currentAttemptNo,
            GenerationRetryDecision retryDecision
    ) {
        log.warn(
                "Requeued timed out generation job: jobId={}, documentId={}, format={}, "
                        + "attemptNo={}, timedOutWorkerId={}, recoveryWorkerId={}, "
                        + "recoveryWorkerName={}, retryAction={}",
                job.getId(),
                job.getDocumentId(),
                job.getFormat(),
                currentAttemptNo,
                job.getLockedBy(),
                generationWorkerIdentityProvider.getWorkerId(),
                generationWorkerIdentityProvider.getExecutionName(),
                retryDecision.action()
        );
    }

    private void logTimedOutJobFailure(GenerationJob job, int currentAttemptNo) {
        log.error(
                "Generation job timed out and exhausted retries: jobId={}, documentId={}, "
                        + "format={}, attemptNo={}, timedOutWorkerId={}, recoveryWorkerId={}, "
                        + "recoveryWorkerName={}",
                job.getId(),
                job.getDocumentId(),
                job.getFormat(),
                currentAttemptNo,
                job.getLockedBy(),
                generationWorkerIdentityProvider.getWorkerId(),
                generationWorkerIdentityProvider.getExecutionName()
        );
    }
}
