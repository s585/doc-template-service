package ru.sberbank.sbercrm.saas.doctemplate.document.usecase;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import ru.sberbank.sbercrm.saas.doctemplate.application.exception.CrmErrorCodes;
import ru.sberbank.sbercrm.saas.doctemplate.application.exception.model.NotFoundCrmException;
import ru.sberbank.sbercrm.saas.doctemplate.application.exception.model.SystemCrmException;
import ru.sberbank.sbercrm.saas.doctemplate.application.integration.client.FileRs;
import ru.sberbank.sbercrm.saas.doctemplate.application.integration.gateway.FileStorageGateway;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.CollectionDataset;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.GenerationArtifactMeta;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.GenerationErrorDecision;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.GeneratedFileResult;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.GenerationJob;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.GenerationJobAttempt;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.GenerationJobPreAttemptContext;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.GenerationRetryDecision;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.GenerationTemplateContext;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.GenerationTransitionContext;
import ru.sberbank.sbercrm.saas.doctemplate.document.service.GenerationErrorClassifier;
import ru.sberbank.sbercrm.saas.doctemplate.document.service.GenerationJobAttemptService;
import ru.sberbank.sbercrm.saas.doctemplate.document.service.GenerationJobTransitionService;
import ru.sberbank.sbercrm.saas.doctemplate.document.service.GenerationRetryPolicy;
import ru.sberbank.sbercrm.saas.doctemplate.document.service.GenerationWorkerIdentityProvider;
import ru.sberbank.sbercrm.saas.doctemplate.document.service.context.GenerationContextAssembler;
import ru.sberbank.sbercrm.saas.doctemplate.template.constant.TemplateConstants;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.InMemoryMultipartFile;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.Template;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.TemplateFormat;
import ru.sberbank.sbercrm.saas.doctemplate.template.processor.TemplateProcessingFacade;
import ru.sberbank.sbercrm.saas.doctemplate.template.properties.DocTemplateProperties;
import ru.sberbank.sbercrm.saas.doctemplate.template.service.TemplateService;

@Slf4j
@Component
@RequiredArgsConstructor
public class GenerationJobExecutionUseCaseImpl implements GenerationJobExecutionUseCase {
    private final GenerationErrorClassifier generationErrorClassifier;
    private final GenerationRetryPolicy generationRetryPolicy;
    private final TemplateService templateService;
    private final GenerationJobAttemptService generationJobAttemptService;
    private final GenerationContextAssembler generationContextAssembler;
    private final FileStorageGateway fileStorageGateway;
    private final TemplateProcessingFacade templateProcessingFacade;
    private final DocTemplateProperties docTemplateProperties;
    private final GenerationJobTransitionService generationJobTransitionService;
    private final GenerationWorkerIdentityProvider generationWorkerIdentityProvider;

    @Override
    public void execute(GenerationJob job) {
        processSafely(job);
    }

    private void processSafely(GenerationJob job) {
        UUID effectiveUserId = job.getCreatedBy();
        GenerationJobAttempt attempt = null;
        try {
            attempt = generationJobTransitionService.startGenerationAttempt(job, effectiveUserId);
            logStartedJob(job, attempt);
            process(job, effectiveUserId, attempt.getId(), attempt.getAttemptNo());
        } catch (Exception ex) {
            if (attempt == null) {
                handleAttemptInitializationFailure(job, effectiveUserId, ex);
                return;
            }
            handleFailure(job, effectiveUserId, attempt, ex);
        }
    }

    private void process(GenerationJob job, UUID effectiveUserId, UUID attemptId, int attemptNo) {
        Template template = templateService
            .findAggregateById(job.getTenantId(), job.getTemplateId())
            .orElseThrow(() -> new NotFoundCrmException(
                TemplateConstants.ErrorCodes.TEMPLATE_NOT_FOUND,
                TemplateConstants.ErrorCodes.TEMPLATE_NOT_FOUND,
                job.getTemplateId()
            ));
        Optional<GenerationArtifactMeta> existingArtifact = findExistingGeneratedArtifact(job, attemptNo);
        if (existingArtifact.isPresent()) {
            completeWithExistingArtifact(job, effectiveUserId, attemptId, attemptNo, existingArtifact.get());
            return;
        }

        logTemplateDownload(job, attemptId, attemptNo, template.getS3Key());
        byte[] templateContent = fileStorageGateway.download(job.getTenantId(), effectiveUserId, template.getS3Key());
        logTemplateDownloaded(job, attemptId, attemptNo, templateContent.length);
        GenerationTemplateContext generationContext = generationContextAssembler.assemble(job, effectiveUserId, template);
        logContextAssembled(job, attemptId, attemptNo, generationContext);
        String generatedFileName = generationContext.getGeneratedFileName();
        byte[] generatedContent = templateProcessingFacade.generate(
            template.getFormat(),
            templateContent,
            generationContext
        );
        logContentGenerated(job, attemptId, attemptNo, generatedFileName, generatedContent.length);
        FileRs uploadedFile = fileStorageGateway.upload(
            job.getTenantId(),
            effectiveUserId,
            buildGeneratedFolderPath(job),
            template.getDescription(),
            buildMultipartFile(generatedFileName, template.getFormat(), generatedContent)
        );
        String checksum = calculateChecksum(generatedContent);
        long sizeBytes = generatedContent.length;
        logGeneratedFileUploaded(job, attemptId, attemptNo, uploadedFile.getKey(), checksum, sizeBytes);
        GenerationTransitionContext context = buildTransitionContext(job, effectiveUserId, attemptId, attemptNo);
        GenerationArtifactMeta artifactMeta = GenerationArtifactMeta.builder()
            .s3Key(uploadedFile.getKey())
            .checksum(checksum)
            .sizeBytes(sizeBytes)
            .build();
        tryPersistUploadedArtifact(context, artifactMeta);
        generationJobTransitionService.completeGeneration(
            context,
            GeneratedFileResult.builder()
                .s3Key(uploadedFile.getKey())
                .checksum(checksum)
                .sizeBytes(sizeBytes)
                .build()
        );
        logCompletedJob(job, attemptId, attemptNo, uploadedFile.getKey());
    }

    private void completeWithExistingArtifact(
        GenerationJob job,
        UUID effectiveUserId,
        UUID attemptId,
        int attemptNo,
        GenerationArtifactMeta existingArtifact
    ) {
        String checksum = existingArtifact.checksum();
        Long sizeBytes = existingArtifact.sizeBytes();
        if (checksum == null || sizeBytes == null) {
            throw new SystemCrmException(
                CrmErrorCodes.SYSTEM_UNEXPECTED,
                CrmErrorCodes.SYSTEM_UNEXPECTED,
                buildMissingArtifactMetaExceptionContext(job.getId(), attemptId, existingArtifact.s3Key())
            );
        }
        GenerationTransitionContext context = buildTransitionContext(job, effectiveUserId, attemptId, attemptNo);
        generationJobTransitionService.completeGeneration(
            context,
            GeneratedFileResult.builder()
                .s3Key(existingArtifact.s3Key())
                .checksum(checksum)
                .sizeBytes(sizeBytes)
                .build()
        );
        logReusedArtifact(job, attemptId, attemptNo, existingArtifact.s3Key());
    }

    private void handleFailure(GenerationJob job, UUID effectiveUserId, GenerationJobAttempt attempt, Exception ex) {
        GenerationRetryDecision retryDecision = buildRetryDecision(attempt, ex);
        GenerationTransitionContext context =
            buildTransitionContext(job, effectiveUserId, attempt.getId(), attempt.getAttemptNo());

        if (retryDecision.action().isRetry()) {
            generationJobTransitionService.retryGeneration(context, retryDecision);
            logRetry(job, attempt, retryDecision, ex);
            return;
        }

        generationJobTransitionService.failGeneration(context, retryDecision);
        logPermanentFailure(job, attempt, retryDecision, ex);
    }

    private void handleAttemptInitializationFailure(GenerationJob job, UUID effectiveUserId, Exception ex) {
        int currentAttemptCount = job.getAttemptCount() == null ? 0 : job.getAttemptCount();
        GenerationRetryDecision retryDecision = buildRetryDecision(currentAttemptCount + 1, ex);
        GenerationJobPreAttemptContext context = buildPreAttemptContext(job, effectiveUserId, currentAttemptCount);

        if (retryDecision.action().isRetry()) {
            generationJobTransitionService.scheduleRetryBeforeAttempt(context, retryDecision);
            logRetryWithoutAttempt(job, retryDecision, ex);
            return;
        }

        generationJobTransitionService.failBeforeAttempt(context, retryDecision);
        logPermanentFailureWithoutAttempt(job, retryDecision, ex);
    }

    private MultipartFile buildMultipartFile(String fileName, TemplateFormat format, byte[] content) {
        return new InMemoryMultipartFile(fileName, fileName, resolveContentType(format), content);
    }

    private String resolveContentType(TemplateFormat format) {
        return switch (format) {
            case DOCX -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case XLSX -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        };
    }

    private String buildGeneratedFolderPath(GenerationJob job) {
        return docTemplateProperties.getFileStorage().getFolder() + "/generated/" + job.getEntityId() + "/"
            + job.getObjectId() + "/" + job.getDocumentId();
    }

    private Optional<GenerationArtifactMeta> findExistingGeneratedArtifact(GenerationJob job, int attemptNo) {
        if (attemptNo <= 1) {
            return Optional.empty();
        }
        return generationJobAttemptService.findLatestArtifactBeforeAttempt(job.getId(), attemptNo);
    }

    private void tryPersistUploadedArtifact(GenerationTransitionContext context, GenerationArtifactMeta artifactMeta) {
        try {
            generationJobTransitionService.persistUploadedArtifact(context, artifactMeta);
        } catch (Exception ex) {
            log.warn(
                "Failed to persist uploaded artifact metadata, continue with completion: jobId={}, "
                    + "attemptId={}, attemptNo={}, fileKey={}",
                context.jobId(),
                context.attemptId(),
                context.attemptNo(),
                artifactMeta.s3Key(),
                ex
            );
        }
    }

    private String calculateChecksum(byte[] content) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(content);
            StringBuilder builder = new StringBuilder(digest.length * 2);
            for (byte value : digest) {
                builder.append(String.format("%02x", value));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new SystemCrmException(
                CrmErrorCodes.SYSTEM_UNEXPECTED,
                CrmErrorCodes.SYSTEM_UNEXPECTED,
                ex,
                "SHA-256"
            );
        }
    }

    private String buildMissingArtifactMetaExceptionContext(UUID jobId, UUID attemptId, String s3Key) {
        return "Missing artifact metadata for retry reuse: "
            + "jobId=" + jobId
            + ", attemptId=" + attemptId
            + ", s3Key=" + s3Key;
    }

    private String truncate(String value) {
        return value.length() <= 1024 ? value : value.substring(0, 1024);
    }

    private String fallbackMessage(Exception ex) {
        return ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
    }

    private GenerationRetryDecision buildRetryDecision(GenerationJobAttempt attempt, Exception ex) {
        return buildRetryDecision(attempt.getAttemptNo(), ex);
    }

    private GenerationRetryDecision buildRetryDecision(int attemptNo, Exception ex) {
        GenerationErrorDecision decision = generationErrorClassifier.classify(ex);
        return generationRetryPolicy.decide(
            attemptNo,
            new GenerationErrorDecision(
                decision.errorCode(),
                truncate(resolveErrorMessage(decision, ex)),
                decision.retriable()
            )
        );
    }

    private String resolveErrorMessage(GenerationErrorDecision decision, Exception ex) {
        return decision.errorMessage() == null ? fallbackMessage(ex) : decision.errorMessage();
    }

    private void logStartedJob(GenerationJob job, GenerationJobAttempt attempt) {
        log.info(
            "Started generation job: jobId={}, attemptId={}, attemptNo={}, documentId={}, "
                + "templateId={}, format={}, workerId={}, workerName={}",
            job.getId(),
            attempt.getId(),
            attempt.getAttemptNo(),
            job.getDocumentId(),
            job.getTemplateId(),
            job.getFormat(),
            job.getLockedBy(),
            generationWorkerIdentityProvider.getExecutionName()
        );
    }

    private void logCompletedJob(GenerationJob job, UUID attemptId, int attemptNo, String fileKey) {
        log.info(
            "Completed generation job: jobId={}, attemptId={}, attemptNo={}, documentId={}, "
                + "templateId={}, format={}, workerId={}, workerName={}, fileKey={}",
            job.getId(),
            attemptId,
            attemptNo,
            job.getDocumentId(),
            job.getTemplateId(),
            job.getFormat(),
            job.getLockedBy(),
            generationWorkerIdentityProvider.getExecutionName(),
            fileKey
        );
    }

    private void logTemplateDownload(GenerationJob job, UUID attemptId, int attemptNo, String templateFileKey) {
        log.debug(
            "Downloading template for generation job: jobId={}, attemptId={}, attemptNo={}, "
                + "documentId={}, templateId={}, format={}, templateFileKey={}",
            job.getId(),
            attemptId,
            attemptNo,
            job.getDocumentId(),
            job.getTemplateId(),
            job.getFormat(),
            templateFileKey
        );
    }

    private void logTemplateDownloaded(GenerationJob job, UUID attemptId, int attemptNo, long sizeBytes) {
        log.debug(
            "Downloaded template for generation job: jobId={}, attemptId={}, attemptNo={}, "
                + "documentId={}, templateId={}, format={}, sizeBytes={}",
            job.getId(),
            attemptId,
            attemptNo,
            job.getDocumentId(),
            job.getTemplateId(),
            job.getFormat(),
            sizeBytes
        );
    }

    private void logContextAssembled(
        GenerationJob job,
        UUID attemptId,
        int attemptNo,
        GenerationTemplateContext generationContext
    ) {
        log.debug(
            "Assembled generation context: jobId={}, attemptId={}, attemptNo={}, documentId={}, "
                + "templateId={}, format={}, scalarKeys={}, collections={}, generatedFileName={}",
            job.getId(),
            attemptId,
            attemptNo,
            job.getDocumentId(),
            job.getTemplateId(),
            job.getFormat(),
            generationContext.getScalarValues().keySet(),
            describeCollections(generationContext),
            generationContext.getGeneratedFileName()
        );
    }

    private void logContentGenerated(
        GenerationJob job,
        UUID attemptId,
        int attemptNo,
        String generatedFileName,
        long sizeBytes
    ) {
        log.debug(
            "Generated document content: jobId={}, attemptId={}, attemptNo={}, documentId={}, "
                + "templateId={}, format={}, generatedFileName={}, sizeBytes={}",
            job.getId(),
            attemptId,
            attemptNo,
            job.getDocumentId(),
            job.getTemplateId(),
            job.getFormat(),
            generatedFileName,
            sizeBytes
        );
    }

    private void logGeneratedFileUploaded(
        GenerationJob job,
        UUID attemptId,
        int attemptNo,
        String fileKey,
        String checksum,
        long sizeBytes
    ) {
        log.debug(
            "Uploaded generated file: jobId={}, attemptId={}, attemptNo={}, documentId={}, "
                + "templateId={}, format={}, fileKey={}, checksum={}, sizeBytes={}",
            job.getId(),
            attemptId,
            attemptNo,
            job.getDocumentId(),
            job.getTemplateId(),
            job.getFormat(),
            fileKey,
            checksum,
            sizeBytes
        );
    }

    private List<String> describeCollections(GenerationTemplateContext generationContext) {
        return generationContext.getCollections().stream()
            .map(this::describeCollection)
            .toList();
    }

    private String describeCollection(CollectionDataset dataset) {
        return "keys=" + dataset.getKeys() + ", rowCount=" + dataset.getRows().size();
    }

    private void logReusedArtifact(GenerationJob job, UUID attemptId, int attemptNo, String fileKey) {
        log.info(
            "Completed generation job using existing artifact: jobId={}, attemptId={}, attemptNo={}, "
                + "documentId={}, templateId={}, format={}, workerId={}, workerName={}, fileKey={}",
            job.getId(),
            attemptId,
            attemptNo,
            job.getDocumentId(),
            job.getTemplateId(),
            job.getFormat(),
            generationWorkerIdentityProvider.getWorkerId(),
            generationWorkerIdentityProvider.getExecutionName(),
            fileKey
        );
    }

    private void logRetry(
        GenerationJob job,
        GenerationJobAttempt attempt,
        GenerationRetryDecision retryDecision,
        Exception ex
    ) {
        log.warn(
            "Scheduled retry for generation job: jobId={}, attemptId={}, attemptNo={}, "
                + "documentId={}, templateId={}, format={}, workerId={}, workerName={}, "
                + "retryAction={}, errorCode={}, errorMessage={}",
            job.getId(),
            attempt.getId(),
            attempt.getAttemptNo(),
            job.getDocumentId(),
            job.getTemplateId(),
            job.getFormat(),
            job.getLockedBy(),
            generationWorkerIdentityProvider.getExecutionName(),
            retryDecision.action(),
            retryDecision.errorCode(),
            retryDecision.errorMessage(),
            ex
        );
    }

    private void logPermanentFailure(
        GenerationJob job,
        GenerationJobAttempt attempt,
        GenerationRetryDecision retryDecision,
        Exception ex
    ) {
        log.error(
            "Failed generation job permanently: jobId={}, attemptId={}, attemptNo={}, "
                + "documentId={}, templateId={}, format={}, workerId={}, workerName={}, "
                + "errorCode={}, errorMessage={}",
            job.getId(),
            attempt.getId(),
            attempt.getAttemptNo(),
            job.getDocumentId(),
            job.getTemplateId(),
            job.getFormat(),
            job.getLockedBy(),
            generationWorkerIdentityProvider.getExecutionName(),
            retryDecision.errorCode(),
            retryDecision.errorMessage(),
            ex
        );
    }

    private void logRetryWithoutAttempt(
        GenerationJob job,
        GenerationRetryDecision retryDecision,
        Exception ex
    ) {
        log.warn(
            "Scheduled retry before generation attempt initialization: jobId={}, documentId={}, "
                + "templateId={}, format={}, workerId={}, workerName={}, retryAction={}, "
                + "errorCode={}, errorMessage={}",
            job.getId(),
            job.getDocumentId(),
            job.getTemplateId(),
            job.getFormat(),
            job.getLockedBy(),
            generationWorkerIdentityProvider.getExecutionName(),
            retryDecision.action(),
            retryDecision.errorCode(),
            retryDecision.errorMessage(),
            ex
        );
    }

    private void logPermanentFailureWithoutAttempt(
        GenerationJob job,
        GenerationRetryDecision retryDecision,
        Exception ex
    ) {
        log.error(
            "Failed generation job before attempt initialization: jobId={}, documentId={}, "
                + "templateId={}, format={}, workerId={}, workerName={}, errorCode={}, "
                + "errorMessage={}",
            job.getId(),
            job.getDocumentId(),
            job.getTemplateId(),
            job.getFormat(),
            job.getLockedBy(),
            generationWorkerIdentityProvider.getExecutionName(),
            retryDecision.errorCode(),
            retryDecision.errorMessage(),
            ex
        );
    }

    private GenerationTransitionContext buildTransitionContext(
        GenerationJob job,
        UUID effectiveUserId,
        UUID attemptId,
        int attemptNo
    ) {
        return GenerationTransitionContext.builder()
            .tenantId(job.getTenantId())
            .userId(effectiveUserId)
            .attemptId(attemptId)
            .attemptNo(attemptNo)
            .jobId(job.getId())
            .documentId(job.getDocumentId())
            .format(job.getFormat())
            .build();
    }

    private GenerationJobPreAttemptContext buildPreAttemptContext(
        GenerationJob job,
        UUID effectiveUserId,
        int currentAttemptCount
    ) {
        return GenerationJobPreAttemptContext.builder()
            .tenantId(job.getTenantId())
            .userId(effectiveUserId)
            .jobId(job.getId())
            .documentId(job.getDocumentId())
            .format(job.getFormat())
            .currentAttemptCount(currentAttemptCount)
            .build();
    }
}
