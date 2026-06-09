package ru.sberbank.sbercrm.saas.doctemplate.document.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import ru.sberbank.sbercrm.saas.doctemplate.application.exception.CrmErrorCodes;
import ru.sberbank.sbercrm.saas.doctemplate.application.exception.model.SystemCrmException;
import ru.sberbank.sbercrm.saas.doctemplate.document.constant.DocumentConstants;
import ru.sberbank.sbercrm.saas.doctemplate.document.domain.GenerationJobStateMachine;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.GenerationArtifactMeta;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.GeneratedFileResult;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.GenerationErrorDecision;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.GenerationJob;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.GenerationJobAttempt;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.GenerationJobEvent;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.GenerationJobPreAttemptContext;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.GenerationJobRetryCmd;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.GenerationRetryAction;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.GenerationRetryDecision;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.GenerationJobStatus;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.GenerationTransitionContext;

@ExtendWith(MockitoExtension.class)
class GenerationJobTransitionServiceImplTest {
    private static final UUID TENANT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID JOB_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID DOCUMENT_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID ATTEMPT_ID_COMPLETE = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final UUID ATTEMPT_ID_RETRY = UUID.fromString("66666666-6666-6666-6666-666666666666");
    private static final UUID ATTEMPT_ID_FAIL = UUID.fromString("77777777-7777-7777-7777-777777777777");

    @Mock
    private GenerationJobService generationJobService;

    @Mock
    private GeneratedFileService generatedFileService;

    @Mock
    private GenerationJobAttemptService generationJobAttemptService;

    @Mock
    private GenerationJobStateMachine generationJobStateMachine;

    @Mock
    private GenerationErrorClassifier generationErrorClassifier;

    @Mock
    private GenerationRetryPolicy generationRetryPolicy;

    @Mock
    private GenerationWorkerIdentityProvider generationWorkerIdentityProvider;

    @Mock
    private GenerationJobMetricsPublisher generationJobMetricsPublisher;

    @Mock
    private PlatformTransactionManager transactionManager;

    @InjectMocks
    private GenerationJobTransitionServiceImpl systemUnderTest;

    @Test
    @DisplayName("Transition service claim-ит job через state machine событие CLAIM")
    void givenAvailableSlots_whenClaimNextJobsForProcessing_thenUseStateMachineAndClaimJobs() {
        GenerationJob claimedJob = buildProcessingJob();
        given(generationJobStateMachine.transit(GenerationJobStatus.QUEUED, GenerationJobEvent.CLAIM))
            .willReturn(GenerationJobStatus.PROCESSING);
        given(generationJobService.claimNextJobs(USER_ID, 2)).willReturn(List.of(claimedJob));

        assertThat(systemUnderTest.claimNextJobsForProcessing(USER_ID, 2))
            .containsExactly(claimedJob);

        verify(generationJobStateMachine).transit(GenerationJobStatus.QUEUED, GenerationJobEvent.CLAIM);
        verify(generationJobService).claimNextJobs(USER_ID, 2);
    }

    @Test
    @DisplayName("Transition service атомарно создаёт attempt и переводит файл в PROCESSING")
    void givenClaimedJob_whenStartGenerationAttempt_thenCreateAttemptAndMarkFileProcessing() {
        GenerationJobAttempt attempt = GenerationJobAttempt.builder()
            .id(ATTEMPT_ID_COMPLETE)
            .jobId(JOB_ID)
            .attemptNo(1)
            .build();
        given(generationJobAttemptService.create(USER_ID, JOB_ID, USER_ID)).willReturn(attempt);

        assertThat(
                systemUnderTest.startGenerationAttempt(buildProcessingJob(), USER_ID)
            )
            .isEqualTo(attempt);

        verify(generationJobAttemptService).create(USER_ID, JOB_ID, USER_ID);
        verify(generatedFileService).markProcessing(TENANT_ID, USER_ID, DOCUMENT_ID, "DOCX");
    }

    @Test
    @DisplayName("Transition service транзакционно завершает файл и job после валидного перехода")
    void givenProcessingJob_whenCompleteGeneration_thenUpdateFileAndJob() {
        given(generationJobService.findById(TENANT_ID, JOB_ID)).willReturn(Optional.of(buildProcessingJob()));
        given(generationJobStateMachine.transit(GenerationJobStatus.PROCESSING, GenerationJobEvent.COMPLETE))
            .willReturn(GenerationJobStatus.DONE);
        given(generationJobService.markCompleted(TENANT_ID, USER_ID, JOB_ID, 0, 1)).willReturn(true);

        systemUnderTest.completeGeneration(
            buildTransitionContext(ATTEMPT_ID_COMPLETE),
            GeneratedFileResult.builder()
                .s3Key("key")
                .checksum("checksum")
                .sizeBytes(10L)
                .build()
        );

        verify(generationJobStateMachine).transit(GenerationJobStatus.PROCESSING, GenerationJobEvent.COMPLETE);
        verify(generationJobService).markCompleted(TENANT_ID, USER_ID, JOB_ID, 0, 1);
        verify(generatedFileService).markCompleted(TENANT_ID, USER_ID, DOCUMENT_ID, "DOCX", "key", "checksum", 10L);
        verify(generationJobAttemptService).markCompleted(
            eq(USER_ID),
            any(),
            eq(GenerationArtifactMeta.builder().s3Key("key").checksum("checksum").sizeBytes(10L).build())
        );
    }

    @Test
    @DisplayName("Transition service планирует retry при ошибке до создания attempt")
    void givenAttemptInitializationFailureWithRetry_whenScheduleRetryBeforeAttempt_thenScheduleRetry() {
        GenerationRetryDecision retryDecision = new GenerationRetryDecision(
            GenerationRetryAction.RETRY_LATER,
            "file_storage.request_failed",
            "temporary",
            OffsetDateTime.parse("2026-04-02T12:00:10Z")
        );
        GenerationJobRetryCmd retryCmd = buildRetryCmd(
            USER_ID,
            JOB_ID,
            0,
            1,
            OffsetDateTime.parse("2026-04-02T12:00:10Z"),
            "file_storage.request_failed",
            "temporary"
        );
        given(generationJobService.scheduleRetry(retryCmd)).willReturn(true);

        systemUnderTest.scheduleRetryBeforeAttempt(buildPreAttemptContext(), retryDecision);

        verify(generationJobService).scheduleRetry(retryCmd);
        verify(generatedFileService).markPending(TENANT_ID, USER_ID, DOCUMENT_ID, "DOCX");
        verify(generationJobAttemptService, never()).markFailed(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Transition service финализирует ошибку при сбое до создания attempt")
    void givenAttemptInitializationFailureWithoutRetry_whenFailBeforeAttempt_thenFailJob() {
        GenerationRetryDecision retryDecision = new GenerationRetryDecision(
            GenerationRetryAction.FAIL_FINAL,
            "system.unexpected",
            "boom",
            null
        );
        given(generationJobService.markFailed(
            TENANT_ID,
            USER_ID,
            JOB_ID,
            0,
            1,
            "system.unexpected",
            "boom"
        )).willReturn(true);

        systemUnderTest.failBeforeAttempt(buildPreAttemptContext(), retryDecision);

        verify(generationJobService).markFailed(
            TENANT_ID,
            USER_ID,
            JOB_ID,
            0,
            1,
            "system.unexpected",
            "boom"
        );
        verify(generatedFileService).markFailed(
            TENANT_ID,
            USER_ID,
            DOCUMENT_ID,
            "DOCX",
            "system.unexpected",
            "boom"
        );
        verify(generationJobMetricsPublisher).incrementExhaustedRetries(retryDecision);
        verify(generationJobAttemptService, never()).markFailed(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Transition service возвращает просроченную job в очередь и сбрасывает файл в PENDING")
    void givenTimedOutJob_whenRequeueTimedOutJobs_thenUpdateFileAndJob() {
        mockRecoveryTransaction();
        GenerationJobRetryCmd retryCmd = buildRetryCmd(
            USER_ID,
            JOB_ID,
            0,
            1,
            OffsetDateTime.parse("2026-04-02T12:00:10Z"),
            "generation.job_timeout",
            "Generation job timed out"
        );
        given(generationWorkerIdentityProvider.getWorkerId()).willReturn(USER_ID);
        given(generationWorkerIdentityProvider.getExecutionName()).willReturn("doc-template@host:123:generation-1");
        given(generationJobService.findTimedOutJobs()).willReturn(java.util.List.of(buildProcessingJob()));
        given(generationErrorClassifier.classify(DocumentConstants.ErrorCodes.GENERATION_JOB_TIMEOUT, "Generation job timed out"))
            .willReturn(new GenerationErrorDecision(DocumentConstants.ErrorCodes.GENERATION_JOB_TIMEOUT, "Generation job timed out", true));
        given(
            generationRetryPolicy.decide(
                1,
                new GenerationErrorDecision(
                    DocumentConstants.ErrorCodes.GENERATION_JOB_TIMEOUT,
                    "Generation job timed out",
                    true
                )
            )
        )
            .willReturn(new GenerationRetryDecision(
                GenerationRetryAction.RETRY_LATER,
                DocumentConstants.ErrorCodes.GENERATION_JOB_TIMEOUT,
                "Generation job timed out",
                OffsetDateTime.parse("2026-04-02T12:00:10Z")
            ));
        given(generationJobStateMachine.transit(GenerationJobStatus.PROCESSING, GenerationJobEvent.TIMEOUT))
            .willReturn(GenerationJobStatus.QUEUED);
        given(generationJobService.scheduleRetry(retryCmd)).willReturn(true);

        assertThat(systemUnderTest.requeueTimedOutJobs(USER_ID)).isEqualTo(1);

        verify(generationJobStateMachine).transit(GenerationJobStatus.PROCESSING, GenerationJobEvent.TIMEOUT);
        verify(generationJobAttemptService).markTimedOutActiveAttempt(USER_ID, JOB_ID);
        verify(generatedFileService).markPending(TENANT_ID, USER_ID, DOCUMENT_ID, "DOCX");
        verify(generationJobService).scheduleRetry(retryCmd);
    }

    @Test
    @DisplayName("Transition service финализирует timeout без retry, если лимит попыток исчерпан")
    void givenTimedOutJobWithoutRetry_whenRequeueTimedOutJobs_thenFailJob() {
        mockRecoveryTransaction();
        given(generationWorkerIdentityProvider.getWorkerId()).willReturn(USER_ID);
        given(generationWorkerIdentityProvider.getExecutionName()).willReturn("doc-template@host:123:generation-1");
        given(generationJobService.findTimedOutJobs()).willReturn(java.util.List.of(buildProcessingJob()));
        given(generationErrorClassifier.classify(DocumentConstants.ErrorCodes.GENERATION_JOB_TIMEOUT, "Generation job timed out"))
            .willReturn(new GenerationErrorDecision(DocumentConstants.ErrorCodes.GENERATION_JOB_TIMEOUT, "Generation job timed out", true));
        given(
            generationRetryPolicy.decide(
                1,
                new GenerationErrorDecision(
                    DocumentConstants.ErrorCodes.GENERATION_JOB_TIMEOUT,
                    "Generation job timed out",
                    true
                )
            )
        )
            .willReturn(new GenerationRetryDecision(
                GenerationRetryAction.FAIL_FINAL,
                DocumentConstants.ErrorCodes.GENERATION_JOB_TIMEOUT,
                "Generation job timed out",
                null
            ));
        given(generationJobStateMachine.transit(GenerationJobStatus.PROCESSING, GenerationJobEvent.FAIL))
            .willReturn(GenerationJobStatus.ERROR);
        given(generationJobService.markFailed(
            TENANT_ID,
            USER_ID,
            JOB_ID,
            0,
            1,
            "generation.job_timeout",
            "Generation job timed out"
        )).willReturn(true);

        assertThat(systemUnderTest.requeueTimedOutJobs(USER_ID)).isEqualTo(1);

        verify(generationJobAttemptService).markTimedOutActiveAttempt(USER_ID, JOB_ID);
        verify(generatedFileService).markFailed(
            TENANT_ID,
            USER_ID,
            DOCUMENT_ID,
            "DOCX",
            "generation.job_timeout",
            "Generation job timed out"
        );
        verify(generationJobService).markFailed(TENANT_ID, USER_ID, JOB_ID, 0, 1, "generation.job_timeout", "Generation job timed out");
        verify(generationJobMetricsPublisher).incrementExhaustedRetries(any());
    }

    @Test
    @DisplayName("Transition service переводит retriable ошибку в retry")
    void givenProcessingJob_whenRetryGeneration_thenScheduleRetry() {
        OffsetDateTime nextRetryAt = OffsetDateTime.parse("2026-04-02T12:00:10Z");
        GenerationJobRetryCmd retryCmd = buildRetryCmd(
            USER_ID,
            JOB_ID,
            0,
            1,
            nextRetryAt,
            "file_storage.request_failed",
            "temporary"
        );
        GenerationRetryDecision retryDecision = new GenerationRetryDecision(
            GenerationRetryAction.RETRY_LATER,
            "file_storage.request_failed",
            "temporary",
            nextRetryAt
        );
        given(generationJobService.findById(TENANT_ID, JOB_ID)).willReturn(Optional.of(buildProcessingJob()));
        given(generationJobStateMachine.transit(GenerationJobStatus.PROCESSING, GenerationJobEvent.RETRY))
            .willReturn(GenerationJobStatus.QUEUED);
        given(generationJobService.scheduleRetry(retryCmd)).willReturn(true);

        systemUnderTest.retryGeneration(buildTransitionContext(ATTEMPT_ID_RETRY), retryDecision);

        verify(generatedFileService).markPending(TENANT_ID, USER_ID, DOCUMENT_ID, "DOCX");
        verify(generationJobAttemptService).markFailed(eq(USER_ID), any(), eq("file_storage.request_failed"), eq("temporary"));
        verify(generationJobService).scheduleRetry(retryCmd);
    }

    @Test
    @DisplayName("Transition service сохраняет upload metadata в отдельной транзакции")
    void givenUploadedArtifact_whenPersistUploadedArtifact_thenStoreKeyInAttempt() {
        mockRecoveryTransaction();

        GenerationArtifactMeta artifactMeta = GenerationArtifactMeta.builder()
            .s3Key("generated/key.docx")
            .checksum("checksum")
            .sizeBytes(11L)
            .build();
        systemUnderTest.persistUploadedArtifact(buildTransitionContext(ATTEMPT_ID_COMPLETE), artifactMeta);

        verify(generationJobAttemptService).markArtifactUploaded(USER_ID, ATTEMPT_ID_COMPLETE, artifactMeta);
    }

    @Test
    @DisplayName("Transition service пропускает завершение устаревшей попытки")
    void givenStaleAttempt_whenCompleteGeneration_thenDoNotUpdateFileAndAttempt() {
        given(generationJobService.findById(TENANT_ID, JOB_ID)).willReturn(Optional.of(buildProcessingJob()));
        given(generationJobStateMachine.transit(GenerationJobStatus.PROCESSING, GenerationJobEvent.COMPLETE))
            .willReturn(GenerationJobStatus.DONE);
        given(generationJobService.markCompleted(TENANT_ID, USER_ID, JOB_ID, 0, 1)).willReturn(false);

        systemUnderTest.completeGeneration(
            buildTransitionContext(ATTEMPT_ID_COMPLETE),
            GeneratedFileResult.builder()
                .s3Key("key")
                .checksum("checksum")
                .sizeBytes(10L)
                .build()
        );

        verify(generationJobService).markCompleted(TENANT_ID, USER_ID, JOB_ID, 0, 1);
        verify(generatedFileService, never()).markCompleted(any(), any(), any(), any(), any(), any(), anyLong());
        verify(generationJobAttemptService, never()).markCompleted(any(), any(), any());
    }

    @Test
    @DisplayName("Transition service выбрасывает system unexpected при отсутствии job")
    void givenMissingJob_whenFailGeneration_thenThrowSystemUnexpected() {
        GenerationRetryDecision retryDecision = new GenerationRetryDecision(
            GenerationRetryAction.FAIL_FINAL,
            "err",
            "msg",
            null
        );
        given(generationJobService.findById(TENANT_ID, JOB_ID)).willReturn(Optional.empty());
        GenerationTransitionContext transitionContext = buildTransitionContext(ATTEMPT_ID_FAIL);

        assertThatThrownBy(() -> systemUnderTest.failGeneration(transitionContext, retryDecision))
            .isInstanceOf(SystemCrmException.class)
            .extracting("code")
            .isEqualTo(CrmErrorCodes.SYSTEM_UNEXPECTED);

        verify(generationJobStateMachine, never()).transit(any(), any());
        verify(generationJobAttemptService, never()).markFailed(any(), any(), any(), any());
        verify(generatedFileService, never()).markFailed(any(), any(), any(), any(), any(), any());
        verify(generationJobService, never()).markFailed(any(), any(), any(), anyInt(), anyInt(), any(), any());
    }

    @Test
    @DisplayName("Transition service не обновляет таблицы при недопустимом переходе")
    void givenInvalidTransition_whenFailGeneration_thenDoNotUpdateTables() {
        GenerationRetryDecision retryDecision = new GenerationRetryDecision(
            GenerationRetryAction.FAIL_FINAL,
            "err",
            "msg",
            null
        );
        given(generationJobService.findById(TENANT_ID, JOB_ID)).willReturn(Optional.of(buildProcessingJob()));
        given(generationJobStateMachine.transit(GenerationJobStatus.PROCESSING, GenerationJobEvent.FAIL))
            .willThrow(new SystemCrmException(
                DocumentConstants.ErrorCodes.GENERATION_JOB_TRANSITION_INVALID,
                DocumentConstants.ErrorCodes.GENERATION_JOB_TRANSITION_INVALID
            ));
        GenerationTransitionContext transitionContext = buildTransitionContext(ATTEMPT_ID_FAIL);

        assertThatThrownBy(
                () -> systemUnderTest.failGeneration(transitionContext, retryDecision)
            )
            .isInstanceOf(SystemCrmException.class)
            .extracting("code")
            .isEqualTo(DocumentConstants.ErrorCodes.GENERATION_JOB_TRANSITION_INVALID);

        verify(generationJobStateMachine).transit(GenerationJobStatus.PROCESSING, GenerationJobEvent.FAIL);
        verify(generationJobAttemptService, never()).markFailed(any(), any(), any(), any());
        verify(generatedFileService, never()).markFailed(any(), any(), any(), any(), any(), any());
        verify(generationJobService, never()).markFailed(any(), any(), any(), anyInt(), anyInt(), any(), any());
    }

    @Test
    @DisplayName("Transition service не делает requeue при недопустимом TIMEOUT переходе")
    void givenInvalidTimeoutTransition_whenRequeueTimedOutJobs_thenDoNotUpdateTables() {
        mockRecoveryTransaction();
        given(generationJobService.findTimedOutJobs()).willReturn(java.util.List.of(buildProcessingJob()));
        given(generationErrorClassifier.classify(DocumentConstants.ErrorCodes.GENERATION_JOB_TIMEOUT, "Generation job timed out"))
            .willReturn(new GenerationErrorDecision(DocumentConstants.ErrorCodes.GENERATION_JOB_TIMEOUT, "Generation job timed out", true));
        given(
            generationRetryPolicy.decide(
                1,
                new GenerationErrorDecision(
                    DocumentConstants.ErrorCodes.GENERATION_JOB_TIMEOUT,
                    "Generation job timed out",
                    true
                )
            )
        )
            .willReturn(new GenerationRetryDecision(
                GenerationRetryAction.RETRY_LATER,
                DocumentConstants.ErrorCodes.GENERATION_JOB_TIMEOUT,
                "Generation job timed out",
                OffsetDateTime.parse("2026-04-02T12:00:10Z")
            ));
        given(generationJobStateMachine.transit(GenerationJobStatus.PROCESSING, GenerationJobEvent.TIMEOUT))
            .willThrow(new SystemCrmException(
                DocumentConstants.ErrorCodes.GENERATION_JOB_TRANSITION_INVALID,
                DocumentConstants.ErrorCodes.GENERATION_JOB_TRANSITION_INVALID
            ));

        assertThat(systemUnderTest.requeueTimedOutJobs(USER_ID)).isZero();

        verify(generationJobMetricsPublisher).incrementRecoveryFailure();
        verify(generationJobAttemptService, never()).markTimedOutActiveAttempt(any(), any());
        verify(generatedFileService, never()).markPending(any(), any(), any(), any());
        verify(generationJobService, never()).scheduleRetry(any());
    }

    @Test
    @DisplayName("Transition service продолжает recovery остальных job, если одна timed out job падает")
    void givenOneBrokenTimedOutJob_whenRequeueTimedOutJobs_thenContinueWithNextJob() {
        mockRecoveryTransaction();
        UUID secondJobId = UUID.fromString("88888888-1111-2222-3333-444444444444");
        UUID secondDocumentId = UUID.fromString("99999999-1111-2222-3333-444444444444");
        GenerationJobRetryCmd retryCmd = buildRetryCmd(
            USER_ID,
            secondJobId,
            0,
            1,
            OffsetDateTime.parse("2026-04-02T12:00:10Z"),
            "generation.job_timeout",
            "Generation job timed out"
        );
        GenerationJob firstJob = buildProcessingJob();
        GenerationJob secondJob = buildProcessingJob().toBuilder()
            .id(secondJobId)
            .documentId(secondDocumentId)
            .build();

        given(generationWorkerIdentityProvider.getWorkerId()).willReturn(USER_ID);
        given(generationWorkerIdentityProvider.getExecutionName()).willReturn("doc-template@host:123:generation-1");
        given(generationJobService.findTimedOutJobs()).willReturn(java.util.List.of(firstJob, secondJob));
        given(generationErrorClassifier.classify(DocumentConstants.ErrorCodes.GENERATION_JOB_TIMEOUT, "Generation job timed out"))
            .willReturn(new GenerationErrorDecision(DocumentConstants.ErrorCodes.GENERATION_JOB_TIMEOUT, "Generation job timed out", true));
        given(
            generationRetryPolicy.decide(
                1,
                new GenerationErrorDecision(
                    DocumentConstants.ErrorCodes.GENERATION_JOB_TIMEOUT,
                    "Generation job timed out",
                    true
                )
            )
        )
            .willReturn(new GenerationRetryDecision(
                GenerationRetryAction.RETRY_LATER,
                DocumentConstants.ErrorCodes.GENERATION_JOB_TIMEOUT,
                "Generation job timed out",
                OffsetDateTime.parse("2026-04-02T12:00:10Z")
            ));
        given(generationJobStateMachine.transit(GenerationJobStatus.PROCESSING, GenerationJobEvent.TIMEOUT))
            .willThrow(new SystemCrmException(
                DocumentConstants.ErrorCodes.GENERATION_JOB_TRANSITION_INVALID,
                DocumentConstants.ErrorCodes.GENERATION_JOB_TRANSITION_INVALID
            ))
            .willReturn(GenerationJobStatus.QUEUED);
        given(generationJobService.scheduleRetry(retryCmd)).willReturn(true);

        assertThat(systemUnderTest.requeueTimedOutJobs(USER_ID)).isEqualTo(1);

        verify(generationJobMetricsPublisher).incrementRecoveryFailure();
        verify(generationJobAttemptService).markTimedOutActiveAttempt(USER_ID, secondJobId);
        verify(generatedFileService).markPending(TENANT_ID, USER_ID, secondDocumentId, "DOCX");
    }

    private GenerationJob buildProcessingJob() {
        return GenerationJob.builder()
            .id(JOB_ID)
            .tenantId(TENANT_ID)
            .documentId(DOCUMENT_ID)
            .format("DOCX")
            .attemptCount(0)
            .lockedBy(USER_ID)
            .status(GenerationJobStatus.PROCESSING.name())
            .build();
    }

    private GenerationTransitionContext buildTransitionContext(UUID attemptId) {
        return GenerationTransitionContext.builder()
            .tenantId(TENANT_ID)
            .userId(USER_ID)
            .attemptId(attemptId)
            .attemptNo(1)
            .jobId(JOB_ID)
            .documentId(DOCUMENT_ID)
            .format("DOCX")
            .build();
    }

    private GenerationJobPreAttemptContext buildPreAttemptContext() {
        return GenerationJobPreAttemptContext.builder()
            .tenantId(TENANT_ID)
            .userId(USER_ID)
            .jobId(JOB_ID)
            .documentId(DOCUMENT_ID)
            .format("DOCX")
            .currentAttemptCount(0)
            .build();
    }

    private GenerationJobRetryCmd buildRetryCmd(
        UUID userId,
        UUID jobId,
        int expectedAttemptCount,
        int attemptCount,
        OffsetDateTime nextRetryAt,
        String errorCode,
        String errorMessage
    ) {
        return GenerationJobRetryCmd.builder()
            .tenantId(TENANT_ID)
            .userId(userId)
            .jobId(jobId)
            .expectedAttemptCount(expectedAttemptCount)
            .attemptCount(attemptCount)
            .nextRetryAt(nextRetryAt)
            .errorCode(errorCode)
            .errorMessage(errorMessage)
            .build();
    }

    private void mockRecoveryTransaction() {
        given(transactionManager.getTransaction(any())).willReturn(new SimpleTransactionStatus());
    }
}
