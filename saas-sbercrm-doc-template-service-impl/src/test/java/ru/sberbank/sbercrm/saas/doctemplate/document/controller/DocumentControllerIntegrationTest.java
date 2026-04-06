package ru.sberbank.sbercrm.saas.doctemplate.document.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.File;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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
import ru.sberbank.sbercrm.saas.doctemplate.template.properties.TemplateProperties;

@TestPropertySource(properties = {
    "saas.doc-template.file-storage.stub-enabled=true",
    "saas.doc-template.generation.enabled=false"
})
class DocumentControllerIntegrationTest extends AbstractIntegrationTest {
    private static final UUID REQUEST_ID = UUID.fromString("aaaaaaaa-1111-1111-1111-111111111111");
    private static final UUID TEMPLATE_ID_SUFFIX = UUID.fromString("bbbbbbbb-2222-2222-2222-222222222222");
    private static final UUID OBJECT_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");

    @Autowired
    private GenerationJobExecutionUseCase generationJobExecutionUseCase;

    @Autowired
    private GenerationJobService generationJobService;

    @Autowired
    private FileStorageGateway fileStorageGateway;

    @Autowired
    private TemplateProperties templateProperties;

    @Test
    @DisplayName("Генерация документа создает реальный DOCX файл с подставленными значениями")
    void givenDocxTemplate_whenGenerateDocument_thenPersistDoneFileAndRealDocxContent() throws Exception {
        UUID requestId = REQUEST_ID;
        UUID templateIdSuffix = TEMPLATE_ID_SUFFIX;
        String templateKey = "templates/" + ENTITY_ID + "/generation-" + templateIdSuffix + ".docx";
        writeTemplateToStubStorage(templateKey, createDocx("Contract for ${customer_name}"));

        Template template = templateMother.createTemplateWithMappings(
            TENANT_ID,
            USER_ID,
            ENTITY_ID,
            "Шаблон договора",
            "DOC_GENERATION_" + templateIdSuffix,
            "Описание шаблона",
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
                            .source(ConstantValueSource.builder().value("ready-contract").build())
                            .build()
                    )
                    .build(),
                TemplateMapping.builder()
                    .key("customer_name")
                    .definition(
                        TemplateMappingDefinition.builder()
                            .scope(MappingScope.VALUE)
                            .type(TemplateValueType.STRING)
                            .source(ConstantValueSource.builder().value("Romashka LLC").build())
                            .build()
                    )
                    .build()
            )
        );

        DocumentCreationRq request = DocumentCreationRq.builder()
            .templateId(template.getId())
            .entityId(ENTITY_ID)
            .objectId(OBJECT_ID)
            .requestId(requestId)
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
            .andExpect(jsonPath("$.templateId").value(template.getId().toString()))
            .andExpect(jsonPath("$.files.length()").value(1))
            .andExpect(jsonPath("$.files[0].format").value(TemplateFormat.DOCX.value()))
            .andExpect(jsonPath("$.files[0].status").value(DocumentConstants.GeneratedFileStatus.PENDING))
            .andReturn()
            .getResponse()
            .getContentAsString(StandardCharsets.UTF_8);
        DocumentRs createdDocument = objectMapper.readValue(creationResponseBody, DocumentRs.class);

        generationJobExecutionUseCase.execute(
            generationJobService.findByDocumentId(TENANT_ID, createdDocument.getId()).getFirst()
        );

        String getResponseBody = mockMvc.perform(
                get("/v1/doc/{documentId}", createdDocument.getId())
                    .header("X-Tenant-Id", TENANT_ID)
                    .header("X-User-Id", USER_ID)
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(createdDocument.getId().toString()))
            .andExpect(jsonPath("$.files.length()").value(1))
            .andExpect(jsonPath("$.files[0].format").value(TemplateFormat.DOCX.value()))
            .andExpect(jsonPath("$.files[0].status").value(DocumentConstants.GeneratedFileStatus.DONE))
            .andExpect(jsonPath("$.files[0].s3Key").isNotEmpty())
            .andExpect(jsonPath("$.files[0].checksum").isNotEmpty())
            .andExpect(jsonPath("$.files[0].sizeBytes").isNumber())
            .andReturn()
            .getResponse()
            .getContentAsString(StandardCharsets.UTF_8);
        DocumentRs generatedDocument = objectMapper.readValue(getResponseBody, DocumentRs.class);

        String generatedKey = generatedDocument.getFiles().getFirst().getS3Key();
        assertThat(generatedKey).startsWith("doc-template/generated/" + ENTITY_ID + "/" + OBJECT_ID + "/");
        assertThat(generatedKey).endsWith("_ready-contract.docx");

        File generatedFile = fileStorageGateway.download(TENANT_ID, USER_ID, generatedKey);
        String generatedText = readDocxText(generatedFile.toPath());

        assertThat(generatedText).contains("Contract for Romashka LLC");
        assertThat(generatedText).doesNotContain("${customer_name}");
    }

    private void writeTemplateToStubStorage(String key, byte[] content) throws Exception {
        Path filePath = Path.of(templateProperties.getFileStorage().getStubRootPath()).resolve(key);
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

    private String readDocxText(Path path) throws Exception {
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(Files.readAllBytes(path));
             XWPFDocument document = new XWPFDocument(inputStream)) {
            return document.getParagraphs().stream()
                .map(paragraph -> paragraph.getText() == null ? "" : paragraph.getText())
                .reduce("", String::concat);
        }
    }
}
