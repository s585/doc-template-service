package ru.sberbank.sbercrm.saas.doctemplate.document.controller;

import static org.awaitility.Awaitility.await;
import static org.assertj.core.api.Assertions.assertThat;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;
import ru.sberbank.sbercrm.saas.doctemplate.document.constant.DocumentConstants;
import ru.sberbank.sbercrm.saas.doctemplate.document.dto.DocumentRs;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.Template;

@TestPropertySource(properties = {
    "saas.doc-template.file-storage.stub-enabled=true",
    "saas.doc-template.generation.enabled=true",
    "saas.doc-template.generation.dispatch-fixed-delay-ms=50",
    "saas.doc-template.generation.worker-pool-size=1"
})
class DocumentGenerationRuntimeIntegrationTest extends AbstractDocumentGenerationIntegrationTest {
    private static final UUID REQUEST_ID = UUID.fromString("12121212-1111-1111-1111-111111111111");
    private static final UUID TEMPLATE_ID_SUFFIX = UUID.fromString("23232323-2222-2222-2222-222222222222");
    private static final UUID OBJECT_ID = UUID.fromString("34343434-3333-3333-3333-333333333333");

    @Test
    @DisplayName("Шедулер и диспетчер обрабатывают задачу генерации до конечного результата")
    void givenGeneratedDocument_whenSchedulerRuns_thenJobIsClaimedAndFileBecomesDone() throws Exception {
        UUID templateIdSuffix = TEMPLATE_ID_SUFFIX;
        String templateKey = "templates/" + ENTITY_ID + "/runtime-generation-" + templateIdSuffix + ".docx";
        writeTemplateToStubStorage(templateKey, createDocx("Runtime contract for ${customer_name}"));

        Template template = createDocxTemplate(
            templateKey,
            "Шаблон договора runtime",
            "DOC_RUNTIME_" + templateIdSuffix,
            "Описание runtime шаблона",
            "runtime-contract",
            "Runtime LLC"
        );

        DocumentRs createdDocument = createDocument(template.getId(), OBJECT_ID, REQUEST_ID);
        assertThat(createdDocument.getFiles()).hasSize(1);
        assertThat(createdDocument.getFiles().getFirst().getStatus())
            .isEqualTo(DocumentConstants.GeneratedFileStatus.PENDING);

        DocumentRs[] generatedDocumentHolder = new DocumentRs[1];
        await()
            .atMost(Duration.ofSeconds(10))
            .pollInterval(Duration.ofMillis(100))
            .untilAsserted(() -> {
                DocumentRs document = getDocument(createdDocument.getId());
                assertThat(document.getFiles()).hasSize(1);
                assertThat(document.getFiles().getFirst().getStatus())
                    .isEqualTo(DocumentConstants.GeneratedFileStatus.DONE);
                generatedDocumentHolder[0] = document;
            });

        DocumentRs generatedDocument = generatedDocumentHolder[0];

        assertThat(generatedDocument.getFiles()).hasSize(1);
        assertThat(generatedDocument.getFiles().getFirst().getStatus())
            .isEqualTo(DocumentConstants.GeneratedFileStatus.DONE);
        assertThat(generatedDocument.getFiles().getFirst().getS3Key()).isNotBlank();

        byte[] generatedFile = fileStorageGateway.download(
            TENANT_ID,
            USER_ID,
            generatedDocument.getFiles().getFirst().getS3Key()
        );

        assertThat(readDocxText(generatedFile)).contains("Runtime contract for Runtime LLC");
    }
}
