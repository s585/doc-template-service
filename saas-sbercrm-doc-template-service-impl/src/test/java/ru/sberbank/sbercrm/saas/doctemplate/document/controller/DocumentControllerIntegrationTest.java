package ru.sberbank.sbercrm.saas.doctemplate.document.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import ru.sberbank.sbercrm.saas.doctemplate.document.constant.DocumentConstants;
import ru.sberbank.sbercrm.saas.doctemplate.document.dto.DocumentRs;
import ru.sberbank.sbercrm.saas.doctemplate.document.service.GenerationJobService;
import ru.sberbank.sbercrm.saas.doctemplate.document.usecase.GenerationJobExecutionUseCase;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.Template;

@TestPropertySource(properties = {
    "saas.doc-template.file-storage.stub-enabled=true",
    "saas.doc-template.generation.enabled=false"
})
class DocumentControllerIntegrationTest extends AbstractDocumentGenerationIntegrationTest {
    private static final UUID REQUEST_ID = UUID.fromString("aaaaaaaa-1111-1111-1111-111111111111");
    private static final UUID TEMPLATE_ID_SUFFIX = UUID.fromString("bbbbbbbb-2222-2222-2222-222222222222");
    private static final UUID OBJECT_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");

    @Autowired
    private GenerationJobExecutionUseCase generationJobExecutionUseCase;

    @Autowired
    private GenerationJobService generationJobService;

    @Test
    @DisplayName("Генерация документа создает реальный DOCX файл с подставленными значениями")
    void givenDocxTemplate_whenGenerateDocument_thenPersistDoneFileAndRealDocxContent() throws Exception {
        UUID requestId = REQUEST_ID;
        UUID templateIdSuffix = TEMPLATE_ID_SUFFIX;
        String templateKey = "templates/" + ENTITY_ID + "/generation-" + templateIdSuffix + ".docx";
        writeTemplateToStubStorage(templateKey, createDocx("Contract for ${customer_name}"));

        Template template = createDocxTemplate(
            templateKey,
            "Шаблон договора",
            "DOC_GENERATION_" + templateIdSuffix,
            "Описание шаблона",
            "ready-contract",
            "Romashka LLC"
        );

        DocumentRs createdDocument = createDocument(template.getId(), OBJECT_ID, requestId);
        assertThat(createdDocument.getTemplateId()).isEqualTo(template.getId());
        assertThat(createdDocument.getFiles()).hasSize(1);
        assertThat(createdDocument.getFiles().getFirst().getStatus())
            .isEqualTo(DocumentConstants.GeneratedFileStatus.PENDING);

        generationJobExecutionUseCase.execute(
            generationJobService.findByDocumentId(TENANT_ID, createdDocument.getId()).getFirst()
        );

        DocumentRs generatedDocument = getDocument(createdDocument.getId());

        assertThat(generatedDocument.getId()).isEqualTo(createdDocument.getId());
        assertThat(generatedDocument.getFiles()).hasSize(1);
        assertThat(generatedDocument.getFiles().getFirst().getStatus())
            .isEqualTo(DocumentConstants.GeneratedFileStatus.DONE);
        assertThat(generatedDocument.getFiles().getFirst().getS3Key()).isNotBlank();
        assertThat(generatedDocument.getFiles().getFirst().getChecksum()).isNotBlank();
        assertThat(generatedDocument.getFiles().getFirst().getSizeBytes()).isPositive();
        String generatedKey = generatedDocument.getFiles().getFirst().getS3Key();
        assertThat(generatedKey).startsWith("doc-template/generated/" + ENTITY_ID + "/" + OBJECT_ID + "/");
        assertThat(generatedKey).endsWith("_ready-contract.docx");

        byte[] generatedFile = fileStorageGateway.download(TENANT_ID, USER_ID, generatedKey);
        String generatedText = readDocxText(generatedFile);

        assertThat(generatedText).contains("Contract for Romashka LLC");
        assertThat(generatedText).doesNotContain("${customer_name}");
    }
}
