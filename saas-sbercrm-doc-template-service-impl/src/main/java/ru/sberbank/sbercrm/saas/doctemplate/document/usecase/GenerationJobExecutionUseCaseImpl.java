package ru.sberbank.sbercrm.saas.doctemplate.document.usecase;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import ru.sberbank.sbercrm.saas.doctemplate.application.exception.CrmErrorCodes;
import ru.sberbank.sbercrm.saas.doctemplate.application.exception.model.BusinessCrmException;
import ru.sberbank.sbercrm.saas.doctemplate.application.exception.model.NotFoundCrmException;
import ru.sberbank.sbercrm.saas.doctemplate.application.exception.model.SystemCrmException;
import ru.sberbank.sbercrm.saas.doctemplate.application.integration.client.FileRs;
import ru.sberbank.sbercrm.saas.doctemplate.application.integration.gateway.FileStorageGateway;
import ru.sberbank.sbercrm.saas.doctemplate.document.constant.DocumentConstants;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.GenerationJob;
import ru.sberbank.sbercrm.saas.doctemplate.document.service.GeneratedFileService;
import ru.sberbank.sbercrm.saas.doctemplate.document.service.GenerationJobService;
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
    private final GenerationJobService generationJobService;
    private final GeneratedFileService generatedFileService;
    private final TemplateService templateService;
    private final FileStorageGateway fileStorageGateway;
    private final TemplateProcessingFacade templateProcessingFacade;
    private final DocTemplateProperties docTemplateProperties;

    @Override
    public void execute(GenerationJob job) {
        processSafely(job);
    }

    private void processSafely(GenerationJob job) {
        UUID effectiveUserId = job.getCreatedBy();
        generatedFileService.markProcessing(job.getTenantId(), effectiveUserId, job.getDocumentId(), job.getFormat());
        try {
            process(job, effectiveUserId);
        } catch (Exception ex) {
            log.error("Generation job failed: jobId={}", job.getId(), ex);
            String errorMessage = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
            generatedFileService.markFailed(
                job.getTenantId(),
                effectiveUserId,
                job.getDocumentId(),
                job.getFormat(),
                CrmErrorCodes.SYSTEM_UNEXPECTED,
                truncate(errorMessage)
            );
            generationJobService.markFailed(
                job.getTenantId(),
                effectiveUserId,
                job.getId(),
                CrmErrorCodes.SYSTEM_UNEXPECTED,
                truncate(errorMessage)
            );
        }
    }

    private void process(GenerationJob job, UUID effectiveUserId) {
        Template template = templateService.findAggregateById(job.getTenantId(), job.getTemplateId())
            .orElseThrow(() -> new NotFoundCrmException(TemplateConstants.ErrorCodes.TEMPLATE_NOT_FOUND, job.getTemplateId()));
        byte[] templateContent = fileStorageGateway.download(job.getTenantId(), effectiveUserId, template.getS3Key());
        Map<String, String> values = resolveValues(template);
        byte[] generatedContent = templateProcessingFacade.generate(template.getFormat(), templateContent, values);
        String fileName = resolveGeneratedFileName(template);
        FileRs uploadedFile = fileStorageGateway.upload(
            job.getTenantId(),
            effectiveUserId,
            buildGeneratedFolderPath(job),
            template.getDescription(),
            buildMultipartFile(fileName, template.getFormat(), generatedContent)
        );
        generatedFileService.markCompleted(
            job.getTenantId(),
            effectiveUserId,
            job.getDocumentId(),
            job.getFormat(),
            uploadedFile.getKey(),
            calculateChecksum(generatedContent),
            generatedContent.length
        );
        generationJobService.markCompleted(job.getTenantId(), effectiveUserId, job.getId());
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
        String baseName = template.getMappings() == null ? null : template.getMappings().stream()
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
                mapping.getKey()
            );
        };
    }

    private String buildGeneratedFolderPath(GenerationJob job) {
        return docTemplateProperties.getFileStorage().getFolder()
            + "/generated/"
            + job.getEntityId()
            + "/"
            + job.getObjectId();
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
            throw new SystemCrmException(ex, CrmErrorCodes.SYSTEM_UNEXPECTED, "SHA-256");
        }
    }

    private String truncate(String value) {
        return value.length() <= 1024 ? value : value.substring(0, 1024);
    }
}
