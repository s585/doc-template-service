package ru.sberbank.sbercrm.saas.doctemplate.template.usecase;

import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ru.sberbank.sbercrm.saas.doctemplate.application.exception.CrmErrorCodes;
import ru.sberbank.sbercrm.saas.doctemplate.template.constant.TemplateConstants;
import ru.sberbank.sbercrm.saas.doctemplate.application.exception.model.AbstractCrmException;
import ru.sberbank.sbercrm.saas.doctemplate.application.exception.model.BusinessCrmException;
import ru.sberbank.sbercrm.saas.doctemplate.application.exception.model.SystemCrmException;
import ru.sberbank.sbercrm.saas.doctemplate.template.gateway.filestorage.FileRs;
import ru.sberbank.sbercrm.saas.doctemplate.template.gateway.filestorage.FileStorageGateway;
import ru.sberbank.sbercrm.saas.doctemplate.template.properties.TemplateProperties;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.TemplateVariableInfo;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.InMemoryMultipartFile;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.TemplateCreationCmd;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.MappingScope;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.Template;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.TemplateFormat;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.TemplateMapping;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.TemplateMappingDefinition;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.TemplateValueType;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.source.ConstantValueSource;
import ru.sberbank.sbercrm.saas.doctemplate.template.processor.TemplateProcessingFacade;
import ru.sberbank.sbercrm.saas.doctemplate.template.service.TemplateService;
import ru.sberbank.sbercrm.saas.doctemplate.template.util.TemplateFileUtils;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
public class TemplateImportUseCaseImpl implements TemplateImportUseCase {
    private final TemplateService templateService;
    private final FileStorageGateway fileStorageGateway;
    private final TemplateProperties templateProperties;
    private final TemplateProcessingFacade templateProcessingFacade;

    @Override
    @Transactional
    public Template execute(UUID tenantId, UUID userId, TemplateCreationCmd request, MultipartFile file) {
        templateService.checkCodeUnique(tenantId, request.getCode(), null);

        String originalFileName = TemplateFileUtils.resolveOriginalFileName(file);
        TemplateFormat format = TemplateFileUtils.resolveFormat(originalFileName);
        byte[] content = TemplateFileUtils.readBytes(file);
        List<TemplateMapping> mappings = buildMappings(request.getName(), format, content);
        String folderPath = buildFolderPath(request.getEntityId());

        FileRs uploadedFile = fileStorageGateway.upload(
            tenantId,
            userId,
            folderPath,
            request.getDescription(),
            new InMemoryMultipartFile(
                file.getName(),
                originalFileName,
                file.getContentType(),
                content
            )
        );
        String uploadedKey = uploadedFile.getKey();

        try {
            Template template = templateService.create(
                tenantId,
                Template.builder()
                    .entityId(request.getEntityId())
                    .name(request.getName())
                    .code(request.getCode())
                    .description(request.getDescription())
                    .format(format)
                    .s3Key(uploadedKey)
                    .active(true)
                    .createdBy(userId)
                    .updatedBy(userId)
                    .build()
            );

            templateService.createMappings(tenantId, template.getId(), userId, mappings);

            template.setMappings(templateService.getMappings(tenantId, template.getId()));
            return template;
        } catch (AbstractCrmException ex) {
            rollbackUploadedFile(tenantId, userId, uploadedKey);
            throw ex;
        } catch (RuntimeException ex) {
            rollbackUploadedFile(tenantId, userId, uploadedKey);
            throw new SystemCrmException(ex, CrmErrorCodes.SYSTEM_UNEXPECTED, ex.getClass().getSimpleName());
        }
    }

    private void rollbackUploadedFile(UUID tenantId, UUID userId, String key) {
        if (key == null || key.isBlank()) {
            return;
        }
        try {
            fileStorageGateway.deleteFile(tenantId, userId, key);
        } catch (AbstractCrmException ex) {
            log.warn("Failed to rollback uploaded file with key={}", key, ex);
        }
    }

    private String buildFolderPath(UUID entityId) {
        return "%s/%s".formatted(templateProperties.getFileStorage().getFolder(), entityId);
    }

    private List<TemplateMapping> buildMappings(String templateName, TemplateFormat format, byte[] content) {
        Map<String, MappingScope> variableToScope = new LinkedHashMap<>();
        for (TemplateVariableInfo occurrence : templateProcessingFacade.extractVariables(format, content)) {
            MappingScope currentScope = variableToScope.get(occurrence.getKey());
            if (currentScope != null && currentScope != occurrence.getScope()) {
                throw new BusinessCrmException(TemplateConstants.ErrorCodes.TEMPLATE_VARIABLE_INVALID, occurrence.getKey());
            }
            variableToScope.putIfAbsent(occurrence.getKey(), occurrence.getScope());
        }

        List<TemplateMapping> mappings = new java.util.ArrayList<>();
        mappings.add(
            TemplateMapping.builder()
                .key(TemplateConstants.MappingKeys.GENERATED_FILE_NAME)
                .definition(
                    TemplateMappingDefinition.builder()
                        .scope(MappingScope.FILE_NAME)
                        .type(TemplateValueType.STRING)
                        .source(ConstantValueSource.builder().value(templateName).build())
                        .build()
                )
                .build()
        );

        mappings.addAll(
            variableToScope.entrySet().stream()
                .map(entry -> TemplateMapping.builder()
                    .key(entry.getKey())
                    .definition(
                        TemplateMappingDefinition.builder()
                            .scope(entry.getValue())
                            .build()
                    )
                    .build())
                .toList()
        );
        return mappings;
    }

}
