package ru.sberbank.sbercrm.saas.doctemplate.document.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import ru.sberbank.sbercrm.saas.doctemplate.AbstractIntegrationTest;
import ru.sberbank.sbercrm.saas.doctemplate.application.integration.gateway.FileStorageGateway;
import ru.sberbank.sbercrm.saas.doctemplate.document.dto.DocumentCreationRq;
import ru.sberbank.sbercrm.saas.doctemplate.document.dto.DocumentRs;
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
import ru.sberbank.sbercrm.saas.doctemplate.template.model.source.DirectValueSource;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.source.ReferenceValueSource;
import ru.sberbank.sbercrm.saas.doctemplate.template.properties.FileStorageProperties;

abstract class AbstractDocumentGenerationIntegrationTest extends AbstractIntegrationTest {
    @Autowired
    protected FileStorageGateway fileStorageGateway;

    @Autowired
    protected FileStorageProperties fileStorageProperties;

    protected Template createDocxTemplate(
        String templateKey,
        String templateName,
        String templateCode,
        String templateDescription,
        String generatedFileName,
        String customerName
    ) {
        return createDocxTemplateWithMappings(
            templateKey,
            templateName,
            templateCode,
            templateDescription,
            List.of(
                buildGeneratedFileNameMapping(generatedFileName),
                buildConstantValueMapping("customer_name", customerName)
            )
        );
    }

    protected Template createDocxTemplateWithDirectCustomerName(
        String templateKey,
        String templateName,
        String templateCode,
        String templateDescription,
        String generatedFileName,
        String customerNamePath
    ) {
        return createDocxTemplateWithMappings(
            templateKey,
            templateName,
            templateCode,
            templateDescription,
            List.of(
                buildGeneratedFileNameMapping(generatedFileName),
                buildDirectValueMapping("customer_name", customerNamePath)
            )
        );
    }

    protected Template createDocxTemplateWithMappings(
        String templateKey,
        String templateName,
        String templateCode,
        String templateDescription,
        List<TemplateMapping> mappings
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
            mappings
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
        StubStorageTestUtils.writeToStubStorage(fileStorageProperties, key, content);
    }

    protected void deleteTemplateFromStubStorage(String key) throws Exception {
        StubStorageTestUtils.deleteFromStubStorage(fileStorageProperties, key);
    }

    protected byte[] createDocx(String text) throws Exception {
        return DocxTestUtils.createDocx(text);
    }

    protected byte[] createDocxListItem(String text) throws Exception {
        return DocxTestUtils.createDocxListItem(text);
    }

    protected byte[] createDocxTable(List<String> headerCells, List<String> templateCells) throws Exception {
        return DocxTestUtils.createDocxTable(headerCells, templateCells);
    }

    protected String readDocxText(byte[] content) throws Exception {
        return DocxTestUtils.readDocxText(content);
    }

    protected TemplateMapping buildGeneratedFileNameMapping(String generatedFileName) {
        return buildConstantMapping(
            TemplateConstants.MappingKeys.GENERATED_FILE_NAME,
            MappingScope.FILE_NAME,
            generatedFileName
        );
    }

    protected TemplateMapping buildConstantValueMapping(String key, String value) {
        return buildConstantMapping(key, MappingScope.VALUE, value);
    }

    protected TemplateMapping buildDirectValueMapping(String key, String path) {
        return TemplateMapping.builder()
            .key(key)
            .definition(
                TemplateMappingDefinition.builder()
                    .scope(MappingScope.VALUE)
                    .type(TemplateValueType.STRING)
                    .source(DirectValueSource.builder().path(path).build())
                    .build()
            )
            .build();
    }

    protected TemplateMapping buildReferenceValueMapping(
        String key,
        UUID entityId,
        String targetPath,
        String referenceFieldName,
        String referenceValuePath,
        String path
    ) {
        return TemplateMapping.builder()
            .key(key)
            .definition(
                TemplateMappingDefinition.builder()
                    .scope(MappingScope.COLLECTION)
                    .type(TemplateValueType.STRING)
                    .source(
                        ReferenceValueSource.builder()
                            .entityId(entityId)
                            .targetPath(targetPath)
                            .referenceFieldName(referenceFieldName)
                            .referenceValuePath(referenceValuePath)
                            .path(path)
                            .build()
                    )
                    .build()
            )
            .build();
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
