package ru.sberbank.sbercrm.saas.doctemplate.document.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import ru.sberbank.sbercrm.saas.doctemplate.document.dto.DocumentRs;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.GeneratedFileStatus;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.GenerationJobAttemptStatus;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.GenerationJobStatus;
import ru.sberbank.sbercrm.saas.doctemplate.document.service.GenerationJobAttemptService;
import ru.sberbank.sbercrm.saas.doctemplate.document.service.GenerationJobService;
import ru.sberbank.sbercrm.saas.doctemplate.document.service.GenerationJobTransitionService;
import ru.sberbank.sbercrm.saas.doctemplate.document.usecase.GenerationJobExecutionUseCase;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.Template;

@TestPropertySource(properties = {
    "saas.doc-template.file-storage.local.enabled=true",
    "saas.doc-template.generation.scheduler-enabled=false",
    "saas.doc-template.generation.retry-backoff-seconds=0,0,0,0"
})
class DocumentGenerationRetryIntegrationTest extends AbstractDocumentGenerationIntegrationTest {
    private static final UUID OBJECT_ID = UUID.fromString("66666666-0000-0000-0000-000000000001");
    private static final UUID REQUEST_ID = UUID.fromString("66666666-0000-0000-0000-000000000002");
    private static final UUID TEMPLATE_SUFFIX = UUID.fromString("66666666-0000-0000-0000-000000000003");
    private static final UUID FIRST_WORKER_ID = UUID.fromString("66666666-0000-0000-0000-000000000004");
    private static final UUID SECOND_WORKER_ID = UUID.fromString("66666666-0000-0000-0000-000000000005");

    @Autowired
    private GenerationJobExecutionUseCase generationJobExecutionUseCase;

    @Autowired
    private GenerationJobService generationJobService;

    @Autowired
    private GenerationJobTransitionService generationJobTransitionService;

    @Autowired
    private GenerationJobAttemptService generationJobAttemptService;

    @Test
    @DisplayName("Retry flow переводит job из PROCESSING в QUEUED после ошибки и затем завершает генерацию")
    void givenRetriableFailure_whenRetryThenGenerationCompletes() throws Exception {
        String templateKey = "templates/" + ENTITY_ID + "/retry-" + TEMPLATE_SUFFIX + ".docx";
        deleteTemplateFromStubStorage(templateKey);
        Template template = createDocxTemplate(
            templateKey,
            "Шаблон с retry",
            "DOC_RETRY_" + TEMPLATE_SUFFIX,
            "Описание шаблона с retry",
            "retry-contract",
            "Retry LLC"
        );
        DocumentRs createdDocument = createDocument(template.getId(), OBJECT_ID, REQUEST_ID);

        var firstClaimedJob = generationJobTransitionService.claimNextJobsForProcessing(FIRST_WORKER_ID, 1).getFirst();
        generationJobExecutionUseCase.execute(firstClaimedJob);

        assertThat(generationJobService.findById(TENANT_ID, firstClaimedJob.getId()))
            .get()
            .satisfies(job -> {
                assertThat(job.getStatus()).isEqualTo(GenerationJobStatus.QUEUED.name());
                assertThat(job.getAttemptCount()).isEqualTo(1);
                assertThat(job.getNextRetryAt()).isNotNull();
                assertThat(job.getErrorCode()).isEqualTo("file_storage.request_failed");
            });
        assertThat(getDocument(createdDocument.getId()).getFiles())
            .singleElement()
            .satisfies(file -> assertThat(file.getStatus()).isEqualTo(GeneratedFileStatus.PENDING.name()));

        writeTemplateToStubStorage(templateKey, createDocx("Contract for ${customer_name}"));

        var secondClaimedJob = generationJobTransitionService.claimNextJobsForProcessing(SECOND_WORKER_ID, 1).getFirst();
        generationJobExecutionUseCase.execute(secondClaimedJob);

        DocumentRs generatedDocument = getDocument(createdDocument.getId());
        assertThat(generatedDocument.getFiles())
            .singleElement()
            .satisfies(file -> assertThat(file.getStatus()).isEqualTo(GeneratedFileStatus.DONE.name()));
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
                tuple(1, GenerationJobAttemptStatus.ERROR.name()),
                tuple(2, GenerationJobAttemptStatus.DONE.name())
            );
        byte[] generatedFile = fileStorageGateway.download(
            TENANT_ID,
            USER_ID,
            generatedDocument.getFiles().getFirst().getS3Key()
        );
        assertThat(readDocxText(generatedFile)).contains("Contract for Retry LLC");
    }
}
