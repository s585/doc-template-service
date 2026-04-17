package ru.sberbank.sbercrm.saas.doctemplate.document.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import ru.sberbank.sbercrm.saas.doctemplate.document.dto.DocumentRs;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.GeneratedFileStatus;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.GenerationJobAttemptStatus;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.GenerationJobStatus;
import ru.sberbank.sbercrm.saas.doctemplate.document.gateway.businessobject.BusinessObjectWireMock;
import ru.sberbank.sbercrm.saas.doctemplate.document.service.GenerationJobAttemptService;
import ru.sberbank.sbercrm.saas.doctemplate.document.service.GenerationJobService;
import ru.sberbank.sbercrm.saas.doctemplate.document.usecase.GenerationJobExecutionUseCase;
import ru.sberbank.sbercrm.saas.doctemplate.template.constant.TemplateConstants;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.MappingScope;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.Template;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.TemplateFormat;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.TemplateMapping;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.TemplateMappingDefinition;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.TemplateValueType;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.source.ConstantValueSource;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.source.ReferenceValueSource;

@TestPropertySource(properties = {
    "saas.doc-template.file-storage.stub-enabled=true",
    "saas.doc-template.generation.scheduler-enabled=false",
    "saas.doc-template.generation.retry-backoff-seconds=0,0,0,0"
})
class DocumentControllerIntegrationTest extends AbstractDocumentGenerationIntegrationTest {
    private static final UUID OBJECT_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID REQUEST_ID_DONE = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final UUID REQUEST_ID_RETRY = UUID.fromString("66666666-6666-6666-6666-666666666666");
    private static final UUID REQUEST_ID_ERROR = UUID.fromString("77777777-7777-7777-7777-777777777777");
    private static final UUID TEMPLATE_SUFFIX_DONE = UUID.fromString("88888888-8888-8888-8888-888888888888");
    private static final UUID TEMPLATE_SUFFIX_RETRY = UUID.fromString("99999999-9999-9999-9999-999999999999");
    private static final UUID TEMPLATE_SUFFIX_ERROR = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID WORKER_ID_DONE = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID WORKER_ID_RETRY_FIRST = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final UUID WORKER_ID_RETRY_SECOND = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
    private static final UUID WORKER_ID_ERROR = UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee");
    private static final UUID WORKER_ID_ERROR_CLAIM_CHECK = UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff");

    @Autowired
    private GenerationJobExecutionUseCase generationJobExecutionUseCase;

    @Autowired
    private GenerationJobService generationJobService;

    @Autowired
    private GenerationJobAttemptService generationJobAttemptService;

    @Test
    @DisplayName("Генерация документа резолвит переменные разных типов и создает итоговый DOCX")
    void givenMixedMappings_whenGenerateDocument_thenPersistDoneFileAndResolvedContent() throws Exception {
        String templateKey = "templates/" + ENTITY_ID + "/generation-" + TEMPLATE_SUFFIX_DONE + ".docx";
        writeTemplateToStubStorage(templateKey, createDocx("Contract for ${customer_name} [${customer_type}]"));
        BusinessObjectWireMock.stubGetObject(
            objectMapper,
            TENANT_ID,
            USER_ID,
            ENTITY_ID,
            OBJECT_ID,
            Map.of(
                "customer",
                Map.of("name", "Business Object LLC")
            )
        );

        Template template = createDocxTemplateWithMappings(
            templateKey,
            "Шаблон договора mixed",
            "DOC_GENERATION_" + TEMPLATE_SUFFIX_DONE,
            "Описание шаблона mixed",
            List.of(
                buildGeneratedFileNameMapping("ready-contract"),
                buildDirectValueMapping("customer_name", "source.customer.name"),
                buildConstantValueMapping("customer_type", "VIP")
            )
        );

        DocumentRs createdDocument = createDocument(template.getId(), OBJECT_ID, REQUEST_ID_DONE);
        assertThat(createdDocument.getTemplateId()).isEqualTo(template.getId());
        assertThat(createdDocument.getFiles()).hasSize(1);
        assertThat(createdDocument.getFiles().getFirst().getStatus())
            .isEqualTo(GeneratedFileStatus.PENDING.name());

        var claimedJob = generationJobService.claimNextJobs(WORKER_ID_DONE, 1).getFirst();
        generationJobExecutionUseCase.execute(claimedJob);

        DocumentRs generatedDocument = getDocument(createdDocument.getId());

        assertThat(generatedDocument.getId()).isEqualTo(createdDocument.getId());
        assertThat(generatedDocument.getFiles()).hasSize(1);
        assertThat(generatedDocument.getFiles().getFirst().getStatus())
            .isEqualTo(GeneratedFileStatus.DONE.name());
        assertThat(generatedDocument.getFiles().getFirst().getS3Key()).isNotBlank();
        assertThat(generatedDocument.getFiles().getFirst().getChecksum()).isNotBlank();
        assertThat(generatedDocument.getFiles().getFirst().getSizeBytes()).isPositive();
        String generatedKey = generatedDocument.getFiles().getFirst().getS3Key();
        assertThat(generatedKey)
            .startsWith(
                "doc-template/generated/"
                    + ENTITY_ID
                    + "/"
                    + OBJECT_ID
                    + "/"
                    + createdDocument.getId()
                    + "/"
            )
            .endsWith("_ready-contract.docx");

        byte[] generatedFile = fileStorageGateway.download(TENANT_ID, USER_ID, generatedKey);
        String generatedText = readDocxText(generatedFile);

        assertThat(generatedText).contains("Contract for Business Object LLC [VIP]");
        assertThat(generatedText).doesNotContain("${customer_name}");
        assertThat(generatedText).doesNotContain("${customer_type}");
        BusinessObjectWireMock.verifyGetObject(TENANT_ID, USER_ID, ENTITY_ID, OBJECT_ID);
        assertThat(generationJobAttemptService.findByJobId(claimedJob.getId()))
            .singleElement()
            .satisfies(attempt -> {
                assertThat(attempt.getAttemptNo()).isEqualTo(1);
                assertThat(attempt.getStatus()).isEqualTo(GenerationJobAttemptStatus.DONE.name());
                assertThat(attempt.getWorkerId()).isNotNull();
                assertThat(attempt.getFinishedAt()).isNotNull();
            });
    }

    @Test
    @DisplayName("Временная ошибка генерации переводит job в retry и следующий запуск завершает файл успешно")
    void givenRetriableFailure_whenRetryThenGenerationCompletes() throws Exception {
        String templateKey = "templates/" + ENTITY_ID + "/retry-" + TEMPLATE_SUFFIX_RETRY + ".docx";
        deleteTemplateFromStubStorage(templateKey);

        Template template = templateMother.createTemplateWithMappings(
            TENANT_ID,
            USER_ID,
            ENTITY_ID,
            "Шаблон с retry",
            "DOC_RETRY_" + TEMPLATE_SUFFIX_RETRY,
            "Описание шаблона с retry",
            TemplateFormat.DOCX,
            templateKey,
            true,
            List.of(
                TemplateMapping.builder()
                    .key(TemplateConstants.MappingKeys.GENERATED_FILE_NAME)
                    .definition(
                        TemplateMappingDefinition.builder()
                            .scope(MappingScope.FILE_NAME)
                            .type(TemplateValueType.STRING)
                            .source(ConstantValueSource.builder().value("retry-contract").build())
                            .build()
                    )
                    .build(),
                TemplateMapping.builder()
                    .key("customer_name")
                    .definition(
                        TemplateMappingDefinition.builder()
                            .scope(MappingScope.VALUE)
                            .type(TemplateValueType.STRING)
                            .source(ConstantValueSource.builder().value("Retry LLC").build())
                            .build()
                    )
                    .build()
            )
        );

        DocumentRs createdDocument = createDocument(template.getId(), REQUEST_ID_RETRY);

        var firstClaimedJob = generationJobService.claimNextJobs(WORKER_ID_RETRY_FIRST, 1).getFirst();
        generationJobExecutionUseCase.execute(firstClaimedJob);

        mockMvc.perform(
                get("/v1/doc/{documentId}", createdDocument.getId())
                    .header("X-Tenant-Id", TENANT_ID)
                    .header("X-User-Id", USER_ID)
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.files[0].status").value(GeneratedFileStatus.PENDING.name()));

        assertThat(generationJobAttemptService.findByJobId(firstClaimedJob.getId()))
            .singleElement()
            .satisfies(attempt -> {
                assertThat(attempt.getAttemptNo()).isEqualTo(1);
                assertThat(attempt.getStatus()).isEqualTo(GenerationJobAttemptStatus.ERROR.name());
                assertThat(attempt.getErrorCode()).isEqualTo("file_storage.request_failed");
            });

        assertThat(generationJobService.findById(TENANT_ID, firstClaimedJob.getId()))
            .get()
            .satisfies(job -> {
                assertThat(job.getStatus()).isEqualTo(GenerationJobStatus.QUEUED.name());
                assertThat(job.getAttemptCount()).isEqualTo(1);
                assertThat(job.getNextRetryAt()).isNotNull();
                assertThat(job.getErrorCode()).isEqualTo("file_storage.request_failed");
            });

        writeTemplateToStubStorage(templateKey, createDocx("Contract for ${customer_name}"));

        var secondClaimedJob = generationJobService.claimNextJobs(WORKER_ID_RETRY_SECOND, 1).getFirst();
        generationJobExecutionUseCase.execute(secondClaimedJob);

        String getResponseBody = mockMvc.perform(
                get("/v1/doc/{documentId}", createdDocument.getId())
                    .header("X-Tenant-Id", TENANT_ID)
                    .header("X-User-Id", USER_ID)
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.files[0].status").value(GeneratedFileStatus.DONE.name()))
            .andReturn()
            .getResponse()
            .getContentAsString(StandardCharsets.UTF_8);
        DocumentRs generatedDocument = objectMapper.readValue(getResponseBody, DocumentRs.class);

        byte[] generatedFile = fileStorageGateway.download(
            TENANT_ID,
            USER_ID,
            generatedDocument.getFiles().getFirst().getS3Key()
        );
        assertThat(readDocxText(generatedFile)).contains("Contract for Retry LLC");

        assertThat(generationJobService.findById(TENANT_ID, firstClaimedJob.getId()))
            .get()
            .satisfies(job -> {
                assertThat(job.getStatus()).isEqualTo(GenerationJobStatus.DONE.name());
                assertThat(job.getAttemptCount()).isEqualTo(2);
                assertThat(job.getNextRetryAt()).isNull();
                assertThat(job.getErrorCode()).isNull();
            });

        assertThat(generationJobAttemptService.findByJobId(firstClaimedJob.getId()))
            .hasSize(2)
            .extracting(attempt -> attempt.getAttemptNo(), attempt -> attempt.getStatus())
            .containsExactly(
                org.assertj.core.groups.Tuple.tuple(1, GenerationJobAttemptStatus.ERROR.name()),
                org.assertj.core.groups.Tuple.tuple(2, GenerationJobAttemptStatus.DONE.name())
            );
    }

    @Test
    @DisplayName("Детерминированная ошибка генерации завершает файл и job в ERROR без повторной попытки")
    void givenNonRetriableFailure_whenExecuteThenFailWithoutRetry() throws Exception {
        String templateKey = "templates/" + ENTITY_ID + "/non-retry-" + TEMPLATE_SUFFIX_ERROR + ".docx";
        writeTemplateToStubStorage(templateKey, createDocx("Contract for ${customer_name}"));

        Template template = templateMother.createTemplateWithMappings(
            TENANT_ID,
            USER_ID,
            ENTITY_ID,
            "Шаблон без retry",
            "DOC_NON_RETRY_" + TEMPLATE_SUFFIX_ERROR,
            "Описание шаблона без retry",
            TemplateFormat.DOCX,
            templateKey,
            true,
            List.of(
                TemplateMapping.builder()
                    .key("customer_name")
                    .definition(
                        TemplateMappingDefinition.builder()
                            .scope(MappingScope.VALUE)
                            .type(TemplateValueType.STRING)
                            .source(
                                ReferenceValueSource.builder()
                                    .path("reference.customer.name")
                                    .build()
                            )
                            .build()
                    )
                    .build()
            )
        );

        DocumentRs createdDocument = createDocument(template.getId(), REQUEST_ID_ERROR);

        var claimedJob = generationJobService.claimNextJobs(WORKER_ID_ERROR, 1).getFirst();
        generationJobExecutionUseCase.execute(claimedJob);

        mockMvc.perform(
                get("/v1/doc/{documentId}", createdDocument.getId())
                    .header("X-Tenant-Id", TENANT_ID)
                    .header("X-User-Id", USER_ID)
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.files[0].status").value(GeneratedFileStatus.ERROR.name()))
            .andExpect(jsonPath("$.files[0].errorCode").value("generation.mapping_source_unsupported"));

        assertThat(generationJobService.findById(TENANT_ID, claimedJob.getId()))
            .get()
            .satisfies(job -> {
                assertThat(job.getStatus()).isEqualTo(GenerationJobStatus.ERROR.name());
                assertThat(job.getAttemptCount()).isEqualTo(1);
                assertThat(job.getNextRetryAt()).isNull();
                assertThat(job.getErrorCode()).isEqualTo("generation.mapping_source_unsupported");
            });

        assertThat(generationJobAttemptService.findByJobId(claimedJob.getId()))
            .singleElement()
            .satisfies(attempt -> {
                assertThat(attempt.getAttemptNo()).isEqualTo(1);
                assertThat(attempt.getStatus()).isEqualTo(GenerationJobAttemptStatus.ERROR.name());
                assertThat(attempt.getErrorCode()).isEqualTo("generation.mapping_source_unsupported");
            });

        assertThat(generationJobService.claimNextJobs(WORKER_ID_ERROR_CLAIM_CHECK, 1))
            .noneMatch(job -> job.getId().equals(claimedJob.getId()));
    }

    private DocumentRs createDocument(UUID templateId, UUID requestId) throws Exception {
        return createDocument(templateId, OBJECT_ID, requestId);
    }
}
