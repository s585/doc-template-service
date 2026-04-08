package ru.sberbank.sbercrm.saas.doctemplate.document.controller;

import static org.awaitility.Awaitility.await;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import ru.sberbank.sbercrm.saas.doctemplate.AbstractIntegrationTest;
import ru.sberbank.sbercrm.saas.doctemplate.application.integration.gateway.FileStorageGateway;
import ru.sberbank.sbercrm.saas.doctemplate.document.constant.DocumentConstants;
import ru.sberbank.sbercrm.saas.doctemplate.document.dto.DocumentCreationRq;
import ru.sberbank.sbercrm.saas.doctemplate.document.dto.DocumentRs;
import ru.sberbank.sbercrm.saas.doctemplate.template.constant.TemplateConstants;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.MappingScope;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.Template;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.TemplateFormat;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.TemplateMapping;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.TemplateMappingDefinition;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.TemplateValueType;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.source.ConstantValueSource;
import ru.sberbank.sbercrm.saas.doctemplate.template.properties.DocTemplateProperties;

@TestPropertySource(properties = {
    "saas.doc-template.file-storage.stub-enabled=true",
    "saas.doc-template.generation.enabled=true",
    "saas.doc-template.generation.dispatch-fixed-delay-ms=50",
    "saas.doc-template.generation.worker-pool-size=1"
})
class DocumentGenerationRuntimeIntegrationTest extends AbstractIntegrationTest {
    private static final UUID REQUEST_ID = UUID.fromString("12121212-1111-1111-1111-111111111111");
    private static final UUID TEMPLATE_ID_SUFFIX = UUID.fromString("23232323-2222-2222-2222-222222222222");
    private static final UUID OBJECT_ID = UUID.fromString("34343434-3333-3333-3333-333333333333");

    @Autowired
    private FileStorageGateway fileStorageGateway;

    @Autowired
    private DocTemplateProperties docTemplateProperties;

    @Test
    @DisplayName("Scheduler and dispatcher process generation job end-to-end")
    void givenGeneratedDocument_whenSchedulerRuns_thenJobIsClaimedAndFileBecomesDone() throws Exception {
        UUID templateIdSuffix = TEMPLATE_ID_SUFFIX;
        String templateKey = "templates/" + ENTITY_ID + "/runtime-generation-" + templateIdSuffix + ".docx";
        writeTemplateToStubStorage(templateKey, createDocx("Runtime contract for ${customer_name}"));

        Template template = templateMother.createTemplateWithMappings(
            TENANT_ID,
            USER_ID,
            ENTITY_ID,
            "Шаблон договора runtime",
            "DOC_RUNTIME_" + templateIdSuffix,
            "Описание runtime шаблона",
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
                            .source(ConstantValueSource.builder().value("runtime-contract").build())
                            .build()
                    )
                    .build(),
                TemplateMapping.builder()
                    .key("customer_name")
                    .definition(
                        TemplateMappingDefinition.builder()
                            .scope(MappingScope.VALUE)
                            .type(TemplateValueType.STRING)
                            .source(ConstantValueSource.builder().value("Runtime LLC").build())
                            .build()
                    )
                    .build()
            )
        );

        DocumentCreationRq request = DocumentCreationRq.builder()
            .templateId(template.getId())
            .entityId(ENTITY_ID)
            .objectId(OBJECT_ID)
            .requestId(REQUEST_ID)
            .formats(List.of(TemplateFormat.DOCX.value()))
            .build();

        String creationResponseBody = mockMvc.perform(
                post("/v1/doc/generated")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsBytes(request))
                    .header("X-Tenant-Id", TENANT_ID)
                    .header("X-User-Id", USER_ID)
            )
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.files.length()").value(1))
            .andExpect(jsonPath("$.files[0].status").value(DocumentConstants.GeneratedFileStatus.PENDING))
            .andReturn()
            .getResponse()
            .getContentAsString(StandardCharsets.UTF_8);
        DocumentRs createdDocument = objectMapper.readValue(creationResponseBody, DocumentRs.class);

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

    private DocumentRs getDocument(UUID documentId) throws Exception {
        String responseBody = mockMvc.perform(
                get("/v1/doc/{documentId}", documentId)
                    .header("X-Tenant-Id", TENANT_ID)
                    .header("X-User-Id", USER_ID)
            )
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString(StandardCharsets.UTF_8);

        return objectMapper.readValue(responseBody, DocumentRs.class);
    }

    private void writeTemplateToStubStorage(String key, byte[] content) throws Exception {
        Path filePath = Path.of(docTemplateProperties.getFileStorage().getStubRootPath()).resolve(key);
        Files.createDirectories(filePath.getParent());
        Files.write(filePath, content);
    }

    private byte[] createDocx(String text) throws Exception {
        try (
            XWPFDocument document = new XWPFDocument();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream()
        ) {
            document.createParagraph().createRun().setText(text);
            document.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private String readDocxText(byte[] content) throws Exception {
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(content);
             XWPFDocument document = new XWPFDocument(inputStream)) {
            return document.getParagraphs().stream()
                .map(paragraph -> paragraph.getText() == null ? "" : paragraph.getText())
                .reduce("", String::concat);
        }
    }
}
