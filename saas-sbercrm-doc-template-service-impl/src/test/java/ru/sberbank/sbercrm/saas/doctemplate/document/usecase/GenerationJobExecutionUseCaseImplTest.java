package ru.sberbank.sbercrm.saas.doctemplate.document.usecase;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

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
import ru.sberbank.sbercrm.saas.doctemplate.application.integration.client.FileRs;
import ru.sberbank.sbercrm.saas.doctemplate.application.integration.gateway.FileStorageGateway;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.GenerationJob;
import ru.sberbank.sbercrm.saas.doctemplate.document.service.GeneratedFileService;
import ru.sberbank.sbercrm.saas.doctemplate.document.service.GenerationJobService;
import ru.sberbank.sbercrm.saas.doctemplate.template.constant.TemplateConstants;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.Template;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.TemplateFormat;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.TemplateMapping;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.TemplateMappingDefinition;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.source.ConstantValueSource;
import ru.sberbank.sbercrm.saas.doctemplate.template.processor.TemplateProcessingFacade;
import ru.sberbank.sbercrm.saas.doctemplate.template.properties.TemplateProperties;
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

    @Mock
    private GenerationJobService generationJobService;

    @Mock
    private GeneratedFileService generatedFileService;

    @Mock
    private TemplateService templateService;

    @Mock
    private FileStorageGateway fileStorageGateway;

    @Mock
    private TemplateProcessingFacade templateProcessingFacade;

    @Mock
    private TemplateProperties templateProperties;

    @Mock
    private TemplateProperties.FileStorage fileStorageProperties;

    @InjectMocks
    private GenerationJobExecutionUseCaseImpl systemUnderTest;

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("Execution use case обрабатывает одну job и завершает файл и job в DONE")
    void givenQueuedJob_whenExecute_thenCompleteGeneration() throws Exception {
        GenerationJob job = buildJob();
        Template template = buildTemplate();
        Path sourceFile = tempDir.resolve("template.docx");
        Files.writeString(sourceFile, "template-content");

        given(templateProperties.getFileStorage()).willReturn(fileStorageProperties);
        given(fileStorageProperties.getFolder()).willReturn("/doc-template");
        given(templateService.findAggregateById(TENANT_ID, TEMPLATE_ID)).willReturn(Optional.of(template));
        given(fileStorageGateway.download(TENANT_ID, USER_ID, "templates/template.docx"))
            .willReturn(
                FileRs.builder()
                    .key("templates/template.docx")
                    .path(sourceFile.toString())
                    .fileName("template.docx")
                    .build()
            );
        given(templateProcessingFacade.generate(eq(TemplateFormat.DOCX), any(), any()))
            .willReturn("generated".getBytes());
        given(fileStorageGateway.upload(eq(TENANT_ID), eq(USER_ID), any(), eq("desc"), any()))
            .willReturn(
                FileRs.builder()
                    .key("generated/result.docx")
                    .path(tempDir.resolve("result.docx").toString())
                    .fileName("result.docx")
                    .build()
            );

        systemUnderTest.execute(job);

        verify(generatedFileService).markProcessing(TENANT_ID, USER_ID, DOCUMENT_ID, "DOCX");
        verify(generatedFileService).markCompleted(
            eq(TENANT_ID),
            eq(USER_ID),
            eq(DOCUMENT_ID),
            eq("DOCX"),
            eq("generated/result.docx"),
            any(),
            eq(9L)
        );
        verify(generationJobService).markCompleted(TENANT_ID, USER_ID, JOB_ID);
        verify(generationJobService, never()).markFailed(eq(TENANT_ID), eq(USER_ID), eq(JOB_ID), any(), any());
    }

    @Test
    @DisplayName("Execution use case помечает файл и job в ERROR при исключении")
    void givenJob_whenExecutionFails_thenMarkFileAndJobFailed() {
        GenerationJob job = buildJob();

        given(templateService.findAggregateById(TENANT_ID, TEMPLATE_ID))
            .willThrow(new IllegalStateException("boom"));

        systemUnderTest.execute(job);

        verify(generatedFileService).markProcessing(TENANT_ID, USER_ID, DOCUMENT_ID, "DOCX");
        verify(generatedFileService).markFailed(
            TENANT_ID,
            USER_ID,
            DOCUMENT_ID,
            "DOCX",
            CrmErrorCodes.SYSTEM_UNEXPECTED,
            "boom"
        );
        verify(generationJobService).markFailed(
            TENANT_ID,
            USER_ID,
            JOB_ID,
            CrmErrorCodes.SYSTEM_UNEXPECTED,
            "boom"
        );
        verify(generationJobService, never()).markCompleted(TENANT_ID, USER_ID, JOB_ID);
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
                        .source(ConstantValueSource.builder().value("123").build())
                        .build())
                    .build(),
                TemplateMapping.builder()
                    .key(TemplateConstants.MappingKeys.GENERATED_FILE_NAME)
                    .definition(TemplateMappingDefinition.builder()
                        .source(ConstantValueSource.builder().value("result").build())
                        .build())
                    .build()
            ))
            .build();
    }
}
