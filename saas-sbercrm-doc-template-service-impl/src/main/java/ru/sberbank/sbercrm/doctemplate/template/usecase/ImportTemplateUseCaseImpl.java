package ru.sberbank.sbercrm.doctemplate.template.usecase;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ru.sberbank.sbercrm.doctemplate.common.constant.CrmErrorCodes;
import ru.sberbank.sbercrm.doctemplate.common.exception.BusinessCrmException;
import ru.sberbank.sbercrm.doctemplate.template.adapter.filestorage.FileRs;
import ru.sberbank.sbercrm.doctemplate.template.adapter.filestorage.FileStorageAdapter;
import ru.sberbank.sbercrm.doctemplate.template.constant.TemplateMappingKeys;
import ru.sberbank.sbercrm.doctemplate.template.config.TemplateProperties;
import ru.sberbank.sbercrm.doctemplate.template.model.TemplateVariableInfo;
import ru.sberbank.sbercrm.doctemplate.template.model.InMemoryMultipartFile;
import ru.sberbank.sbercrm.doctemplate.template.model.TemplateCreationCmd;
import ru.sberbank.sbercrm.doctemplate.template.model.MappingScope;
import ru.sberbank.sbercrm.doctemplate.template.model.Template;
import ru.sberbank.sbercrm.doctemplate.template.model.TemplateFormat;
import ru.sberbank.sbercrm.doctemplate.template.model.TemplateMapping;
import ru.sberbank.sbercrm.doctemplate.template.model.TemplateMappingDefinition;
import ru.sberbank.sbercrm.doctemplate.template.model.TemplateValueType;
import ru.sberbank.sbercrm.doctemplate.template.model.source.ConstantValueSource;
import ru.sberbank.sbercrm.doctemplate.template.processor.TemplateProcessingFacade;
import ru.sberbank.sbercrm.doctemplate.template.service.TemplateService;
import ru.sberbank.sbercrm.doctemplate.template.util.TemplateFileUtils;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ImportTemplateUseCaseImpl implements ImportTemplateUseCase {
    private final TemplateService templateService;
    private final FileStorageAdapter fileStorageAdapter;
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
        fileStorageAdapter.ensureFolderExists(tenantId, userId, folderPath);

        String uploadedKey = null;
        try {
            FileRs uploadedFile = fileStorageAdapter.upload(
                tenantId,
                userId,
                folderPath,
                request.getDescription(),
                originalFileName,
                new InMemoryMultipartFile(
                    file.getName(),
                    originalFileName,
                    file.getContentType(),
                    content
                )
            );
            uploadedKey = uploadedFile.getKey();

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
        } catch (RuntimeException ex) {
            cleanupUploadedFile(tenantId, userId, uploadedKey);
            throw ex;
        }
    }

    private void cleanupUploadedFile(UUID tenantId, UUID userId, String key) {
        if (key == null || key.isBlank()) {
            return;
        }

        try {
            fileStorageAdapter.deleteFile(tenantId, userId, key);
        } catch (RuntimeException ignored) {
            // Cleanup failure is non-blocking for the primary import error.
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
                throw new BusinessCrmException(CrmErrorCodes.TEMPLATE_VARIABLE_INVALID, occurrence.getKey());
            }
            variableToScope.putIfAbsent(occurrence.getKey(), occurrence.getScope());
        }

        List<TemplateMapping> mappings = new java.util.ArrayList<>();
        mappings.add(
            TemplateMapping.builder()
                .key(TemplateMappingKeys.GENERATED_FILE_NAME)
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
