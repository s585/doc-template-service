package ru.sberbank.sbercrm.saas.doctemplate.document.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import ru.sberbank.sbercrm.saas.doctemplate.AbstractIntegrationTest;
import ru.sberbank.sbercrm.saas.doctemplate.application.integration.gateway.FileStorageGateway;
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

abstract class AbstractDocumentGenerationIntegrationTest extends AbstractIntegrationTest {
    @Autowired
    protected FileStorageGateway fileStorageGateway;

    @Autowired
    protected DocTemplateProperties docTemplateProperties;

    protected Template createDocxTemplate(
        String templateKey,
        String templateName,
        String templateCode,
        String templateDescription,
        String generatedFileName,
        String customerName
    ) {
        return templateMother.createTemplateWithMappings(
            TENANT_ID,
            USER_ID,
            ENTITY_ID,
            templateName,
            templateCode,
            templateDescription,
            TemplateFormat.DOCX,
            templateKey,
            true,
            List.of(
                buildFileNameMapping(generatedFileName),
                buildCustomerNameMapping(customerName)
            )
        );
    }

    protected DocumentRs createDocument(UUID templateId, UUID objectId, UUID requestId) throws Exception {
        DocumentCreationRq request = DocumentCreationRq.builder()
            .templateId(templateId)
            .entityId(ENTITY_ID)
            .objectId(objectId)
            .requestId(requestId)
            .formats(List.of(TemplateFormat.DOCX.value()))
            .build();

        String responseBody = mockMvc.perform(
                post("/v1/doc/generated")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsBytes(request))
                    .header("X-Tenant-Id", TENANT_ID)
                    .header("X-User-Id", USER_ID)
            )
            .andExpect(status().isAccepted())
            .andReturn()
            .getResponse()
            .getContentAsString(StandardCharsets.UTF_8);

        return objectMapper.readValue(responseBody, DocumentRs.class);
    }

    protected DocumentRs getDocument(UUID documentId) throws Exception {
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

    protected void writeTemplateToStubStorage(String key, byte[] content) throws Exception {
        Path filePath = Path.of(docTemplateProperties.getFileStorage().getStubRootPath()).resolve(key);
        Files.createDirectories(filePath.getParent());
        Files.write(filePath, content);
    }

    protected void deleteTemplateFromStubStorage(String key) throws Exception {
        Path filePath = Path.of(docTemplateProperties.getFileStorage().getStubRootPath()).resolve(key);
        Files.deleteIfExists(filePath);
    }

    protected byte[] createDocx(String text) throws Exception {
        try (
            XWPFDocument document = new XWPFDocument();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream()
        ) {
            document.createParagraph().createRun().setText(text);
            document.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    protected String readDocxText(byte[] content) throws Exception {
        try (
            ByteArrayInputStream inputStream = new ByteArrayInputStream(content);
            XWPFDocument document = new XWPFDocument(inputStream)
        ) {
            return document.getParagraphs().stream()
                .map(paragraph -> paragraph.getText() == null ? "" : paragraph.getText())
                .reduce("", String::concat);
        }
    }

    private TemplateMapping buildFileNameMapping(String generatedFileName) {
        return TemplateMapping.builder()
            .key(TemplateConstants.MappingKeys.GENERATED_FILE_NAME)
            .definition(
                TemplateMappingDefinition.builder()
                    .scope(MappingScope.FILE_NAME)
                    .type(TemplateValueType.STRING)
                    .source(ConstantValueSource.builder().value(generatedFileName).build())
                    .build()
            )
            .build();
    }

    private TemplateMapping buildCustomerNameMapping(String customerName) {
        return TemplateMapping.builder()
            .key("customer_name")
            .definition(
                TemplateMappingDefinition.builder()
                    .scope(MappingScope.VALUE)
                    .type(TemplateValueType.STRING)
                    .source(ConstantValueSource.builder().value(customerName).build())
                    .build()
            )
            .build();
    }
}
