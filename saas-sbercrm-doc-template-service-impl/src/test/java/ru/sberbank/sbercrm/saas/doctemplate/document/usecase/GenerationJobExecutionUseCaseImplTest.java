package ru.sberbank.sbercrm.saas.doctemplate.document.usecase;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.sberbank.sbercrm.saas.doctemplate.application.exception.CrmErrorCodes;
import ru.sberbank.sbercrm.saas.doctemplate.application.exception.model.SystemCrmException;
import ru.sberbank.sbercrm.saas.doctemplate.application.integration.client.FileRs;
import ru.sberbank.sbercrm.saas.doctemplate.application.integration.gateway.FileStorageGateway;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.GenerationArtifactMeta;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.GenerationTransitionContext;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.GenerationErrorDecision;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.GenerationJob;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.GenerationJobAttempt;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.GenerationJobPreAttemptContext;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.GenerationRetryAction;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.GenerationRetryDecision;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.GenerationTemplateContext;
import ru.sberbank.sbercrm.saas.doctemplate.document.service.GenerationErrorClassifier;
import ru.sberbank.sbercrm.saas.doctemplate.document.service.GenerationJobAttemptService;
import ru.sberbank.sbercrm.saas.doctemplate.document.service.GenerationJobTransitionService;
import ru.sberbank.sbercrm.saas.doctemplate.document.service.GenerationRetryPolicy;
import ru.sberbank.sbercrm.saas.doctemplate.document.service.GenerationWorkerIdentityProvider;
import ru.sberbank.sbercrm.saas.doctemplate.document.service.context.GenerationContextAssembler;
import ru.sberbank.sbercrm.saas.doctemplate.template.constant.TemplateConstants;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.MappingScope;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.Template;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.TemplateFormat;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.TemplateMapping;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.TemplateMappingDefinition;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.TemplateValueType;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.source.ConstantValueSource;
import ru.sberbank.sbercrm.saas.doctemplate.template.processor.TemplateProcessingFacade;
import ru.sberbank.sbercrm.saas.doctemplate.template.properties.DocTemplateProperties;
import ru.sberbank.sbercrm.saas.doctemplate.template.service.TemplateService;

@ExtendWith(MockitoExtension.class)
class GenerationJobExecutionUseCaseImplTest {
    private static final UUID TENANT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID DOCUMENT_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID TEMPLATE_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID ENTITY_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final UUID OBJECT_ID = UUID.fromString("66666666-6666-6666-6666-666666666666");
    private static final UUID JOB_ID = UUID.fromString("77777777-7777-7777-7777-777777777777");
    private static final UUID ATTEMPT_ID = UUID.fromString("88888888-8888-8888-8888-888888888888");

    @Mock
    private GenerationErrorClassifier generationErrorClassifier;

    @Mock
    private GenerationRetryPolicy generationRetryPolicy;

    @Mock
    private TemplateService templateService;

    @Mock
    private GenerationJobAttemptService generationJobAttemptService;

    @Mock
    private GenerationContextAssembler generationContextAssembler;

    @Mock
    private FileStorageGateway fileStorageGateway;

    @Mock
    private TemplateProcessingFacade templateProcessingFacade;

    @Mock
    private DocTemplateProperties docTemplateProperties;

    @Mock
    private DocTemplateProperties.FileStorage fileStorageProperties;

    @Mock
    private GenerationJobTransitionService generationJobTransitionService;

    @Mock
    private GenerationWorkerIdentityProvider generationWorkerIdentityProvider;

    @InjectMocks
    private GenerationJobExecutionUseCaseImpl systemUnderTest;

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("Сценарий исполнения обрабатывает одну задачу и завершает файл и задачу в DONE")
    void givenQueuedJob_whenExecute_thenCompleteGeneration() throws Exception {
        GenerationJob job = buildJob();
        Template template = buildTemplate();
        Path sourceFile = tempDir.resolve("template.docx");
        Files.writeString(sourceFile, "template-content");

        given(generationWorkerIdentityProvider.getExecutionName()).willReturn("worker@host:123:generation-1");
        given(docTemplateProperties.getFileStorage()).willReturn(fileStorageProperties);
        given(fileStorageProperties.getGeneratedFolder()).willReturn("documents");
        given(generationJobTransitionService.startGenerationAttempt(job, USER_ID)).willReturn(buildAttempt(1));
        given(templateService.findAggregateById(TENANT_ID, TEMPLATE_ID)).willReturn(Optional.of(template));
        given(fileStorageGateway.download(TENANT_ID, USER_ID, "templates/template.docx"))
            .willReturn(Files.readAllBytes(sourceFile));
        given(generationContextAssembler.assemble(job, USER_ID, template))
            .willReturn(
                GenerationTemplateContext.builder()
                    .generatedFileName("result.docx")
                    .scalarValues(java.util.Map.of("deal_number", "123"))
                    .build()
            );
        given(templateProcessingFacade.generate(eq(TemplateFormat.DOCX), any(), any()))
            .willReturn("generated".getBytes());
        given(fileStorageGateway.upload(eq(TENANT_ID), eq(USER_ID), any(), eq("desc"), any()))
            .willReturn(
                FileRs.builder()
                    .key("documents/result.docx")
                    .path(tempDir.resolve("result.docx").toString())
                    .fileName("result.docx")
                    .build()
            );

        systemUnderTest.execute(job);

        verify(generationJobTransitionService).startGenerationAttempt(job, USER_ID);
        verify(generationContextAssembler).assemble(job, USER_ID, template);
        verify(generationJobTransitionService).persistUploadedArtifact(
            eq(buildTransitionContext()),
            argThat(meta ->
                "documents/result.docx".equals(meta.s3Key())
                    && meta.checksum() != null
                    && meta.sizeBytes() == 9L
            )
        );
        verify(generationJobTransitionService).completeGeneration(
            eq(buildTransitionContext()),
            argThat(result -> "documents/result.docx".equals(result.s3Key()) && result.sizeBytes() == 9L)
        );
        verify(generationJobTransitionService, never()).failGeneration(eq(buildTransitionContext()), any());
    }

    @Test
    @DisplayName("Use case использует собранный контекст генерации при вызове процессора")
    void givenPreparedContext_whenExecute_thenPassContextValuesToProcessor() throws Exception {
        GenerationJob job = buildJob();
        Template template = buildTemplate();
        Path sourceFile = tempDir.resolve("template-direct.docx");
        Files.writeString(sourceFile, "template-content");

        given(generationWorkerIdentityProvider.getExecutionName()).willReturn("worker@host:123:generation-1");
        given(docTemplateProperties.getFileStorage()).willReturn(fileStorageProperties);
        given(fileStorageProperties.getGeneratedFolder()).willReturn("documents");
        given(generationJobTransitionService.startGenerationAttempt(job, USER_ID)).willReturn(buildAttempt(1));
        given(templateService.findAggregateById(TENANT_ID, TEMPLATE_ID)).willReturn(Optional.of(template));
        given(fileStorageGateway.download(TENANT_ID, USER_ID, "templates/template.docx"))
            .willReturn(Files.readAllBytes(sourceFile));
        given(generationContextAssembler.assemble(job, USER_ID, template))
            .willReturn(
                GenerationTemplateContext.builder()
                    .generatedFileName("direct.docx")
                    .scalarValues(java.util.Map.of("customer_name", "Direct LLC"))
                    .build()
            );
        given(templateProcessingFacade.generate(eq(TemplateFormat.DOCX), any(), any()))
            .willReturn("generated".getBytes());
        given(fileStorageGateway.upload(eq(TENANT_ID), eq(USER_ID), any(), eq("desc"), any()))
            .willReturn(
                FileRs.builder()
                    .key("documents/direct.docx")
                    .path(tempDir.resolve("direct.docx").toString())
                    .fileName("direct.docx")
                    .build()
            );

        systemUnderTest.execute(job);

        verify(generationJobTransitionService).persistUploadedArtifact(
            eq(buildTransitionContext()),
            argThat(meta ->
                "documents/direct.docx".equals(meta.s3Key())
                    && meta.checksum() != null
                    && meta.sizeBytes() == 9L
            )
        );
        verify(templateProcessingFacade).generate(
            eq(TemplateFormat.DOCX),
            any(),
            argThat(context -> "Direct LLC".equals(context.getScalarValues().get("customer_name")))
        );
    }

    @Test
    @DisplayName("Ошибка фиксации upload metadata не прерывает успешное завершение job")
    void givenArtifactPersistFailure_whenExecute_thenStillCompleteGeneration() throws Exception {
        GenerationJob job = buildJob();
        Template template = buildTemplate();
        Path sourceFile = tempDir.resolve("template-artifact-persist.docx");
        Files.writeString(sourceFile, "template-content");

        given(generationWorkerIdentityProvider.getExecutionName()).willReturn("worker@host:123:generation-1");
        given(docTemplateProperties.getFileStorage()).willReturn(fileStorageProperties);
        given(fileStorageProperties.getGeneratedFolder()).willReturn("documents");
        given(generationJobTransitionService.startGenerationAttempt(job, USER_ID)).willReturn(buildAttempt(1));
        given(templateService.findAggregateById(TENANT_ID, TEMPLATE_ID)).willReturn(Optional.of(template));
        given(fileStorageGateway.download(TENANT_ID, USER_ID, "templates/template.docx"))
            .willReturn(Files.readAllBytes(sourceFile));
        given(generationContextAssembler.assemble(job, USER_ID, template))
            .willReturn(
                GenerationTemplateContext.builder()
                    .generatedFileName("result.docx")
                    .scalarValues(java.util.Map.of("deal_number", "123"))
                    .build()
            );
        given(templateProcessingFacade.generate(eq(TemplateFormat.DOCX), any(), any()))
            .willReturn("generated".getBytes());
        given(fileStorageGateway.upload(eq(TENANT_ID), eq(USER_ID), any(), eq("desc"), any()))
            .willReturn(
                FileRs.builder()
                    .key("documents/result.docx")
                    .path(tempDir.resolve("result.docx").toString())
                    .fileName("result.docx")
                    .build()
            );
        willThrow(new RuntimeException("persist failed"))
            .given(generationJobTransitionService)
            .persistUploadedArtifact(
                eq(buildTransitionContext()),
                argThat(meta -> "documents/result.docx".equals(meta.s3Key()))
            );

        systemUnderTest.execute(job);

        verify(generationJobTransitionService).persistUploadedArtifact(
            eq(buildTransitionContext()),
            argThat(meta ->
                "documents/result.docx".equals(meta.s3Key())
                    && meta.checksum() != null
                    && meta.sizeBytes() == 9L
            )
        );
        verify(generationJobTransitionService).completeGeneration(
            eq(buildTransitionContext()),
            argThat(result -> "documents/result.docx".equals(result.s3Key()) && result.sizeBytes() == 9L)
        );
        verify(generationJobTransitionService, never()).retryGeneration(eq(buildTransitionContext()), any());
        verify(generationJobTransitionService, never()).failGeneration(eq(buildTransitionContext()), any());
    }

    @Test
    @DisplayName("Повторная попытка переиспользует ранее загруженный файл и не запускает генерацию заново")
    void givenRetryAttemptWithExistingArtifact_whenExecute_thenReuseStoredFile() throws Exception {
        GenerationJob job = buildJob().toBuilder().attemptCount(1).build();
        GenerationJobAttempt attempt = buildAttempt(2);
        Template template = buildTemplate();
        GenerationArtifactMeta existingArtifact = GenerationArtifactMeta.builder()
            .s3Key("documents/existing.docx")
            .checksum("precalculated-checksum")
            .sizeBytes(8L)
            .build();

        given(generationWorkerIdentityProvider.getExecutionName()).willReturn("worker@host:123:generation-1");
        given(generationJobTransitionService.startGenerationAttempt(job, USER_ID)).willReturn(attempt);
        given(templateService.findAggregateById(TENANT_ID, TEMPLATE_ID)).willReturn(Optional.of(template));
        given(generationJobAttemptService.findLatestArtifactBeforeAttempt(JOB_ID, 2))
            .willReturn(Optional.of(existingArtifact));

        systemUnderTest.execute(job);

        verify(generationJobAttemptService).findLatestArtifactBeforeAttempt(JOB_ID, 2);
        verify(generationJobTransitionService, never()).persistUploadedArtifact(any(), any());
        verify(generationContextAssembler, never()).assemble(any(), any(), any());
        verify(fileStorageGateway, never()).download(TENANT_ID, USER_ID, "documents/existing.docx");
        verify(fileStorageGateway, never()).download(TENANT_ID, USER_ID, "templates/template.docx");
        verify(templateProcessingFacade, never()).generate(any(), any(), any());
        verify(fileStorageGateway, never()).upload(eq(TENANT_ID), eq(USER_ID), any(), any(), any());
        verify(generationJobTransitionService).completeGeneration(
            eq(buildTransitionContext(2)),
            argThat(result ->
                "documents/existing.docx".equals(result.s3Key())
                    && 8L == result.sizeBytes()
                    && "precalculated-checksum".equals(result.checksum())
            )
        );
    }

    @Test
    @DisplayName("Повторная попытка без checksum/size в metadata завершается ошибкой")
    void givenRetryAttemptWithArtifactWithoutMeta_whenExecute_thenFailGeneration() {
        GenerationJob job = buildJob().toBuilder().attemptCount(1).build();
        GenerationJobAttempt attempt = buildAttempt(2);
        Template template = buildTemplate();
        GenerationArtifactMeta existingArtifact = GenerationArtifactMeta.builder()
            .s3Key("documents/existing.docx")
            .build();
        GenerationRetryDecision failDecision = new GenerationRetryDecision(
            GenerationRetryAction.FAIL_FINAL,
            CrmErrorCodes.SYSTEM_UNEXPECTED,
            "Missing artifact metadata for retry reuse",
            null
        );

        given(generationWorkerIdentityProvider.getExecutionName()).willReturn("worker@host:123:generation-1");
        given(generationJobTransitionService.startGenerationAttempt(job, USER_ID)).willReturn(attempt);
        given(templateService.findAggregateById(TENANT_ID, TEMPLATE_ID)).willReturn(Optional.of(template));
        given(generationJobAttemptService.findLatestArtifactBeforeAttempt(JOB_ID, 2))
            .willReturn(Optional.of(existingArtifact));
        given(generationErrorClassifier.classify(any()))
            .willReturn(new GenerationErrorDecision(CrmErrorCodes.SYSTEM_UNEXPECTED, "Missing artifact metadata for retry reuse", false));
        given(generationRetryPolicy.decide(eq(2), any())).willReturn(failDecision);

        systemUnderTest.execute(job);

        verify(generationJobTransitionService).failGeneration(buildTransitionContext(2), failDecision);
        verify(generationJobTransitionService, never()).completeGeneration(eq(buildTransitionContext(2)), any());
    }

    @Test
    @DisplayName("Сценарий исполнения помечает файл и задачу в ERROR при исключении")
    void givenJob_whenExecutionFails_thenMarkFileAndJobFailed() {
        GenerationJob job = buildJob();
        given(generationWorkerIdentityProvider.getExecutionName()).willReturn("worker@host:123:generation-1");
        given(generationJobTransitionService.startGenerationAttempt(job, USER_ID)).willReturn(buildAttempt(1));
        given(generationErrorClassifier.classify(any()))
            .willReturn(
                new GenerationErrorDecision(
                    CrmErrorCodes.SYSTEM_UNEXPECTED,
                    "boom",
                    false
                )
            );
        given(generationRetryPolicy.decide(eq(1), any())).willReturn(
            new GenerationRetryDecision(GenerationRetryAction.FAIL_FINAL, CrmErrorCodes.SYSTEM_UNEXPECTED, "boom", null)
        );

        given(templateService.findAggregateById(TENANT_ID, TEMPLATE_ID))
            .willThrow(new SystemCrmException(CrmErrorCodes.SYSTEM_UNEXPECTED, CrmErrorCodes.SYSTEM_UNEXPECTED));

        systemUnderTest.execute(job);

        verify(generationJobTransitionService).startGenerationAttempt(job, USER_ID);
        verify(generationJobTransitionService).failGeneration(
            buildTransitionContext(),
            new GenerationRetryDecision(GenerationRetryAction.FAIL_FINAL, CrmErrorCodes.SYSTEM_UNEXPECTED, "boom", null)
        );
        verify(generationJobTransitionService, never()).retryGeneration(eq(buildTransitionContext()), any());
        verify(generationJobTransitionService, never()).completeGeneration(eq(buildTransitionContext()), any());
    }

    @Test
    @DisplayName("Execution use case планирует retry для retriable ошибки")
    void givenRetriableFailure_whenExecute_thenScheduleRetry() {
        GenerationJob job = buildJob();
        GenerationRetryDecision retryDecision = new GenerationRetryDecision(
            GenerationRetryAction.RETRY_LATER,
            CrmErrorCodes.FILE_STORAGE_REQUEST_FAILED,
            "temporary",
            null
        );
        given(generationWorkerIdentityProvider.getExecutionName()).willReturn("worker@host:123:generation-1");
        given(generationJobTransitionService.startGenerationAttempt(job, USER_ID)).willReturn(buildAttempt(1));
        given(generationErrorClassifier.classify(any()))
            .willReturn(
                new GenerationErrorDecision(
                    CrmErrorCodes.FILE_STORAGE_REQUEST_FAILED,
                    "temporary",
                    true
                )
            );
        given(generationRetryPolicy.decide(eq(1), any())).willReturn(retryDecision);
        given(templateService.findAggregateById(TENANT_ID, TEMPLATE_ID))
            .willThrow(new SystemCrmException(
                CrmErrorCodes.FILE_STORAGE_REQUEST_FAILED,
                CrmErrorCodes.FILE_STORAGE_REQUEST_FAILED
            ));

        systemUnderTest.execute(job);

        verify(generationJobTransitionService).retryGeneration(
            buildTransitionContext(),
            retryDecision
        );
        verify(generationJobTransitionService, never()).failGeneration(eq(buildTransitionContext()), any());
    }

    @Test
    @DisplayName("Execution use case планирует retry при сбое до создания attempt")
    void givenAttemptInitializationFailure_whenExecute_thenScheduleRetryDirectly() {
        GenerationJob job = buildJob();
        GenerationRetryDecision retryDecision = new GenerationRetryDecision(
            GenerationRetryAction.RETRY_LATER,
            CrmErrorCodes.FILE_STORAGE_REQUEST_FAILED,
            "temporary",
            null
        );
        given(generationWorkerIdentityProvider.getExecutionName()).willReturn("worker@host:123:generation-1");
        given(generationJobTransitionService.startGenerationAttempt(job, USER_ID))
            .willThrow(new SystemCrmException(
                CrmErrorCodes.FILE_STORAGE_REQUEST_FAILED,
                CrmErrorCodes.FILE_STORAGE_REQUEST_FAILED
            ));
        given(generationErrorClassifier.classify(any()))
            .willReturn(new GenerationErrorDecision(CrmErrorCodes.FILE_STORAGE_REQUEST_FAILED, "temporary", true));
        given(generationRetryPolicy.decide(eq(1), any())).willReturn(retryDecision);
        systemUnderTest.execute(job);

        verify(generationJobTransitionService).scheduleRetryBeforeAttempt(
            buildPreAttemptContext(),
            retryDecision
        );
        verify(generationJobTransitionService, never()).retryGeneration(eq(buildTransitionContext()), any());
    }

    @Test
    @DisplayName("Execution use case финализирует ошибку при сбое до создания attempt")
    void givenAttemptInitializationFailureWithoutRetry_whenExecute_thenFailDirectly() {
        GenerationJob job = buildJob();
        GenerationRetryDecision retryDecision = new GenerationRetryDecision(
            GenerationRetryAction.FAIL_FINAL,
            CrmErrorCodes.SYSTEM_UNEXPECTED,
            "boom",
            null
        );
        given(generationWorkerIdentityProvider.getExecutionName()).willReturn("worker@host:123:generation-1");
        given(generationJobTransitionService.startGenerationAttempt(job, USER_ID))
            .willThrow(new SystemCrmException(CrmErrorCodes.SYSTEM_UNEXPECTED, CrmErrorCodes.SYSTEM_UNEXPECTED));
        given(generationErrorClassifier.classify(any()))
            .willReturn(new GenerationErrorDecision(CrmErrorCodes.SYSTEM_UNEXPECTED, "boom", false));
        given(generationRetryPolicy.decide(eq(1), any())).willReturn(retryDecision);

        systemUnderTest.execute(job);

        verify(generationJobTransitionService).failBeforeAttempt(
            buildPreAttemptContext(),
            retryDecision
        );
        verify(generationJobTransitionService, never()).retryGeneration(eq(buildTransitionContext()), any());
        verify(generationJobTransitionService, never()).failGeneration(eq(buildTransitionContext()), any());
    }

    private GenerationJob buildJob() {
        return GenerationJob.builder()
            .id(JOB_ID)
            .tenantId(TENANT_ID)
            .documentId(DOCUMENT_ID)
            .templateId(TEMPLATE_ID)
            .entityId(ENTITY_ID)
            .objectId(OBJECT_ID)
            .format("DOCX")
            .createdBy(USER_ID)
            .updatedBy(USER_ID)
            .lockedBy(USER_ID)
            .build();
    }

    private GenerationJobAttempt buildAttempt(int attemptNo) {
        return GenerationJobAttempt.builder()
            .id(ATTEMPT_ID)
            .jobId(JOB_ID)
            .attemptNo(attemptNo)
            .workerId(USER_ID)
            .status("PROCESSING")
            .build();
    }

    private Template buildTemplate() {
        return Template.builder()
            .id(TEMPLATE_ID)
            .name("contract")
            .description("desc")
            .format(TemplateFormat.DOCX)
            .s3Key("templates/template.docx")
            .mappings(List.of(
                TemplateMapping.builder()
                    .key("deal_number")
                    .definition(TemplateMappingDefinition.builder()
                        .scope(MappingScope.VALUE)
                        .type(TemplateValueType.STRING)
                        .source(ConstantValueSource.builder().value("123").build())
                        .build())
                    .build(),
                TemplateMapping.builder()
                    .key(TemplateConstants.MappingKeys.GENERATED_FILE_NAME)
                    .definition(TemplateMappingDefinition.builder()
                        .scope(MappingScope.FILE_NAME)
                        .type(TemplateValueType.STRING)
                        .source(ConstantValueSource.builder().value("result").build())
                        .build())
                    .build()
            ))
            .build();
    }

    private GenerationTransitionContext buildTransitionContext() {
        return buildTransitionContext(1);
    }

    private GenerationTransitionContext buildTransitionContext(int attemptNo) {
        return GenerationTransitionContext.builder()
            .tenantId(TENANT_ID)
            .userId(USER_ID)
            .attemptId(ATTEMPT_ID)
            .attemptNo(attemptNo)
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
}
