package ru.sberbank.sbercrm.saas.doctemplate.document.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.TestPropertySource;
import ru.sberbank.sbercrm.saas.doctemplate.AbstractIntegrationTest;
import ru.sberbank.sbercrm.saas.doctemplate.application.integration.gateway.FileStorageGateway;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.DocumentCreationCmd;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.GenerationJobAttemptStatus;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.GenerationJobStatus;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.GeneratedFileStatus;
import ru.sberbank.sbercrm.saas.doctemplate.document.service.GenerationJobAttemptService;
import ru.sberbank.sbercrm.saas.doctemplate.document.service.GenerationJobService;
import ru.sberbank.sbercrm.saas.doctemplate.document.service.GenerationJobTransitionService;
import ru.sberbank.sbercrm.saas.doctemplate.document.support.DocxTestUtils;
import ru.sberbank.sbercrm.saas.doctemplate.document.support.StubStorageTestUtils;
import ru.sberbank.sbercrm.saas.doctemplate.template.constant.TemplateConstants;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.MappingScope;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.Template;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.TemplateFormat;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.TemplateMapping;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.TemplateMappingDefinition;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.TemplateValueType;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.source.ConstantValueSource;
import ru.sberbank.sbercrm.saas.doctemplate.template.properties.FileStorageProperties;

@TestPropertySource(properties = {
    "saas.doc-template.file-storage.local.enabled=true",
    "saas.doc-template.generation.scheduler-enabled=false",
    "saas.doc-template.generation.retry-backoff-seconds=0,0,0,0"
})
class GenerationJobExecutionUseCaseIntegrationTest extends AbstractIntegrationTest {
    private static final UUID OBJECT_ID = UUID.fromString("d3000000-0000-0000-0000-000000000003");
    private static final UUID REQUEST_ID = UUID.fromString("e3000000-0000-0000-0000-000000000003");
    private static final UUID WORKER_ID = UUID.fromString("f3000000-0000-0000-0000-000000000003");

    @Autowired
    private DocumentCreationUseCase documentCreationUseCase;

    @Autowired
    private DocumentGetUseCase documentGetUseCase;

    @Autowired
    private GenerationJobService generationJobService;

    @Autowired
    private GenerationJobAttemptService generationJobAttemptService;

    @Autowired
    private FileStorageGateway fileStorageGateway;

    @Autowired
    private FileStorageProperties fileStorageProperties;

    @SpyBean
    private GenerationJobTransitionService generationJobTransitionService;

    @Autowired
    private GenerationJobExecutionUseCase systemUnderTest;

    @Test
    @DisplayName("Если не удаётся сохранить информацию о загруженном артефакте, задача доводится до завершения")
    void givenArtifactPersistFailure_whenExecute_thenGenerationStillCompletes() throws Exception {
        String templateKey = "templates/" + ENTITY_ID + "/execute-persist-fail.docx";
        StubStorageTestUtils.writeToStubStorage(
            fileStorageProperties,
            templateKey,
            DocxTestUtils.createDocx("Contract for ${customer_name}")
        );
        Template template = templateMother.createTemplateWithMappings(
            TENANT_ID,
            USER_ID,
            ENTITY_ID,
            "Шаблон execute persist fail",
            "EXECUTE_PERSIST_FAIL_" + UUID.randomUUID(),
            "desc",
            TemplateFormat.DOCX,
            templateKey,
            true,
            List.of(
                buildGeneratedFileNameMapping("ready-contract"),
                buildConstantValueMapping("customer_name", "Retry LLC")
            )
        );
        var createdDocument = documentCreationUseCase.execute(
            TENANT_ID,
            USER_ID,
            DocumentCreationCmd.builder()
                .templateId(template.getId())
                .entityId(ENTITY_ID)
                .objectId(OBJECT_ID)
                .requestId(REQUEST_ID)
                .formats(List.of(TemplateFormat.DOCX.value()))
                .build()
        );
        var claimedJob = generationJobService.claimNextJobs(WORKER_ID, 1).getFirst();

        willThrow(new RuntimeException("persist failed"))
            .given(generationJobTransitionService)
            .persistUploadedArtifact(any(), any());

        systemUnderTest.execute(claimedJob);

        verify(generationJobTransitionService).persistUploadedArtifact(any(), any());
        assertThat(generationJobService.findById(TENANT_ID, claimedJob.getId()))
            .get()
            .satisfies(job -> {
                assertThat(job.getStatus()).isEqualTo(GenerationJobStatus.DONE.name());
                assertThat(job.getAttemptCount()).isEqualTo(1);
                assertThat(job.getErrorCode()).isNull();
            });
        assertThat(generationJobAttemptService.findByJobId(claimedJob.getId()))
            .singleElement()
            .satisfies(attempt -> {
                assertThat(attempt.getAttemptNo()).isEqualTo(1);
                assertThat(attempt.getStatus()).isEqualTo(GenerationJobAttemptStatus.DONE.name());
            });

        var finalDocument = documentGetUseCase.execute(TENANT_ID, createdDocument.getId());
        assertThat(finalDocument.getFiles())
            .singleElement()
            .satisfies(file -> {
                assertThat(file.getStatus()).isEqualTo(GeneratedFileStatus.DONE.name());
                assertThat(file.getS3Key()).isNotBlank();
                assertThat(file.getChecksum()).isNotBlank();
                assertThat(file.getSizeBytes()).isPositive();
            });

        byte[] generated = fileStorageGateway.download(
            TENANT_ID,
            USER_ID,
            finalDocument.getFiles().getFirst().getS3Key()
        );
        assertThat(DocxTestUtils.readDocxText(generated)).contains("Contract for Retry LLC");
    }

    private TemplateMapping buildGeneratedFileNameMapping(String generatedFileName) {
        return buildConstantMapping(
            TemplateConstants.MappingKeys.GENERATED_FILE_NAME,
            MappingScope.FILE_NAME,
            generatedFileName
        );
    }

    private TemplateMapping buildConstantValueMapping(String key, String value) {
        return buildConstantMapping(key, MappingScope.VALUE, value);
    }

    private TemplateMapping buildConstantMapping(String key, MappingScope scope, String value) {
        return TemplateMapping.builder()
            .key(key)
            .definition(
                TemplateMappingDefinition.builder()
                    .scope(scope)
                    .type(TemplateValueType.STRING)
                    .source(ConstantValueSource.builder().value(value).build())
                    .build()
            )
            .build();
    }
}
