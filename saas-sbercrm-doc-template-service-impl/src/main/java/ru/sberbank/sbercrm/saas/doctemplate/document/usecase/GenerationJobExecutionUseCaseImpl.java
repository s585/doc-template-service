package ru.sberbank.sbercrm.saas.doctemplate.document.usecase;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import ru.sberbank.sbercrm.saas.doctemplate.application.exception.CrmErrorCodes;
import ru.sberbank.sbercrm.saas.doctemplate.application.exception.model.BusinessCrmException;
import ru.sberbank.sbercrm.saas.doctemplate.application.exception.model.NotFoundCrmException;
import ru.sberbank.sbercrm.saas.doctemplate.application.exception.model.SystemCrmException;
import ru.sberbank.sbercrm.saas.doctemplate.application.integration.client.FileFilterRq;
import ru.sberbank.sbercrm.saas.doctemplate.application.integration.client.FileRs;
import ru.sberbank.sbercrm.saas.doctemplate.application.integration.gateway.FileStorageGateway;
import ru.sberbank.sbercrm.saas.doctemplate.document.constant.DocumentConstants;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.GenerationErrorDecision;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.GeneratedFileResult;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.GenerationJob;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.GenerationJobAttempt;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.GenerationJobPreAttemptContext;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.GenerationRetryDecision;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.GenerationTransitionContext;
import ru.sberbank.sbercrm.saas.doctemplate.document.service.GenerationErrorClassifier;
import ru.sberbank.sbercrm.saas.doctemplate.document.service.GenerationJobTransitionService;
import ru.sberbank.sbercrm.saas.doctemplate.document.service.GenerationRetryPolicy;
import ru.sberbank.sbercrm.saas.doctemplate.document.service.GenerationWorkerIdentityProvider;
import ru.sberbank.sbercrm.saas.doctemplate.template.constant.TemplateConstants;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.InMemoryMultipartFile;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.Template;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.TemplateFormat;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.TemplateMapping;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.source.ConstantValueSource;
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
        byte[] templateContent = fileStorageGateway.download(job.getTenantId(), effectiveUserId, template.getS3Key());
        Map<String, String> values = resolveValues(template);
        String generatedFileName = resolveGeneratedFileName(template);
        Optional<FileRs> existingArtifact = findExistingGeneratedArtifact(
            job,
            effectiveUserId,
            attemptNo,
            generatedFileName
        );
        if (existingArtifact.isPresent()) {
            completeWithExistingArtifact(job, effectiveUserId, attemptId, attemptNo, existingArtifact.get());
            return;
        }
        byte[] generatedContent = templateProcessingFacade.generate(template.getFormat(), templateContent, values);
        FileRs uploadedFile = fileStorageGateway.upload(
            job.getTenantId(),
            effectiveUserId,
            buildGeneratedFolderPath(job),
            template.getDescription(),
            buildMultipartFile(generatedFileName, template.getFormat(), generatedContent)
        );
        GenerationTransitionContext context = buildTransitionContext(job, effectiveUserId, attemptId, attemptNo);
        generationJobTransitionService.completeGeneration(
            context,
            GeneratedFileResult.builder()
                .s3Key(uploadedFile.getKey())
                .checksum(calculateChecksum(generatedContent))
                .sizeBytes(generatedContent.length)
                .build()
        );
        logCompletedJob(job, attemptId, attemptNo, uploadedFile.getKey());
    }

    private void completeWithExistingArtifact(
        GenerationJob job,
        UUID effectiveUserId,
        UUID attemptId,
        int attemptNo,
        FileRs existingArtifact
    ) {
        byte[] existingContent = fileStorageGateway.download(job.getTenantId(), effectiveUserId, existingArtifact.getKey());
        GenerationTransitionContext context = buildTransitionContext(job, effectiveUserId, attemptId, attemptNo);
        generationJobTransitionService.completeGeneration(
            context,
            GeneratedFileResult.builder()
                .s3Key(existingArtifact.getKey())
                .checksum(calculateChecksum(existingContent))
                .sizeBytes(resolveArtifactSize(existingArtifact, existingContent))
                .build()
        );
        logReusedArtifact(job, attemptId, attemptNo, existingArtifact.getKey());
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

    private Map<String, String> resolveValues(Template template) {
        Map<String, String> values = new LinkedHashMap<>();
        if (template.getMappings() == null) {
            return values;
        }
        for (TemplateMapping mapping : template.getMappings()) {
            if (TemplateConstants.MappingKeys.GENERATED_FILE_NAME.equals(mapping.getKey())) {
                continue;
            }
            values.put(mapping.getKey(), resolveMappingValue(mapping));
        }
        return values;
    }

    private String resolveGeneratedFileName(Template template) {
        String baseName = template.getMappings() == null
            ? null
            : template.getMappings().stream()
                .filter(mapping -> TemplateConstants.MappingKeys.GENERATED_FILE_NAME.equals(mapping.getKey()))
                .map(this::resolveMappingValue)
                .filter(value -> !value.isBlank())
                .findFirst()
                .orElse(null);
        if (baseName == null || baseName.isBlank()) {
            baseName = template.getName();
        }
        String extension = template.getFormat().value().toLowerCase();
        return baseName.endsWith("." + extension) ? baseName : baseName + "." + extension;
    }

    private String resolveMappingValue(TemplateMapping mapping) {
        if (mapping.getDefinition() == null || mapping.getDefinition().getSource() == null) {
            return "";
        }
        return switch (mapping.getDefinition().getSource()) {
            case ConstantValueSource constantValueSource ->
                constantValueSource.getValue() == null ? "" : String.valueOf(constantValueSource.getValue());
            default -> throw new BusinessCrmException(
                DocumentConstants.ErrorCodes.GENERATION_MAPPING_SOURCE_UNSUPPORTED,
                DocumentConstants.ErrorCodes.GENERATION_MAPPING_SOURCE_UNSUPPORTED,
                mapping.getKey()
            );
        };
    }

    private String buildGeneratedFolderPath(GenerationJob job) {
        return docTemplateProperties.getFileStorage().getFolder() + "/generated/" + job.getEntityId() + "/"
            + job.getObjectId() + "/" + job.getDocumentId();
    }

    private Optional<FileRs> findExistingGeneratedArtifact(
        GenerationJob job,
        UUID effectiveUserId,
        int attemptNo,
        String generatedFileName
    ) {
        if (attemptNo <= 1) {
            return Optional.empty();
        }
        List<FileRs> files = fileStorageGateway.findAllByFilter(
            job.getTenantId(),
            effectiveUserId,
            FileFilterRq.builder()
                .source(docTemplateProperties.getFileStorage().getSource())
                .prefixKey(buildGeneratedFolderPath(job))
                .originalFileName(generatedFileName)
                .build()
        );
        if (CollectionUtils.isEmpty(files)) {
            return Optional.empty();
        }
        if (files.size() > 1) {
            log.warn(
                "Found multiple generated artifacts for retry reuse: jobId={}, documentId={}, "
                    + "attemptNo={}, fileCount={}",
                job.getId(),
                job.getDocumentId(),
                attemptNo,
                files.size()
            );
        }
        return files.stream()
            .filter(file -> file.getKey() != null && !file.getKey().isBlank())
            .max(
                Comparator.comparing(
                    this::resolveArtifactTimestamp,
                    Comparator.nullsLast(Comparator.naturalOrder())
                )
            );
    }

    private OffsetDateTime resolveArtifactTimestamp(FileRs file) {
        return file.getUpdatedDate() != null ? file.getUpdatedDate() : file.getCreatedDate();
    }

    private long resolveArtifactSize(FileRs file, byte[] existingContent) {
        return file.getSize() != null ? file.getSize() : existingContent.length;
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
