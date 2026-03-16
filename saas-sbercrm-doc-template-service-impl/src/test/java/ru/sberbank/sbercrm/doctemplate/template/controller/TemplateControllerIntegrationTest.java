package ru.sberbank.sbercrm.doctemplate.template.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import ru.sberbank.sbercrm.doctemplate.template.TemplateCreationRq;
import ru.sberbank.sbercrm.doctemplate.template.TemplateRs;
import ru.sberbank.sbercrm.doctemplate.template.adapter.filestorage.FileRs;
import ru.sberbank.sbercrm.doctemplate.template.adapter.filestorage.FileStorageClient;
import ru.sberbank.sbercrm.doctemplate.template.adapter.filestorage.FolderRs;
import ru.sberbank.sbercrm.doctemplate.template.constant.TemplateMappingKeys;
import ru.sberbank.sbercrm.doctemplate.template.model.MappingScope;
import ru.sberbank.sbercrm.doctemplate.template.model.Template;
import ru.sberbank.sbercrm.doctemplate.template.model.TemplateMapping;
import ru.sberbank.sbercrm.doctemplate.template.model.TemplateValueType;
import ru.sberbank.sbercrm.doctemplate.template.model.source.ConstantValueSource;
import ru.sberbank.sbercrm.doctemplate.template.service.TemplateService;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@AutoConfigureEmbeddedDatabase
@ActiveProfiles("test")
class TemplateControllerIntegrationTest {
    private static final UUID TENANT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID ENTITY_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TemplateService templateService;

    @MockBean
    private FileStorageClient fileStorageClient;

    @Test
    @DisplayName("Импорт шаблона сохраняет шаблон и маппинги на happy path")
    void givenValidDocxTemplate_whenImportTemplate_thenPersistTemplateAndMappings() throws Exception {
        // given
        String templateName = "Договор поставки";
        String templateDescription = "Тестовый шаблон";
        String templateCode = "SUPPLY_CONTRACT";
        String templateFileName = "import-happy-path.docx";
        String fileStorageSource = "doc-template-service";
        String templateFolderPath = "/doc-template/" + ENTITY_ID;
        String expectedS3Key = "templates/" + ENTITY_ID + "/" + templateFileName;

        TemplateCreationRq request = TemplateCreationRq.builder()
            .entityId(ENTITY_ID)
            .name(templateName)
            .description(templateDescription)
            .code(templateCode)
            .build();
        MockMultipartFile requestPart = new MockMultipartFile(
            "request",
            "",
            MediaType.APPLICATION_JSON_VALUE,
            objectMapper.writeValueAsBytes(request)
        );
        MockMultipartFile filePart = new MockMultipartFile(
            "file",
            templateFileName,
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            new ClassPathResource("template/import-happy-path.docx").getContentAsByteArray()
        );
        given(
            fileStorageClient.getFolder(eq(fileStorageSource), eq(templateFolderPath), eq(TENANT_ID), eq(USER_ID))
        ).willReturn(
            FolderRs.builder()
                .path(templateFolderPath)
                .source(fileStorageSource)
                .build()
        );
        given(fileStorageClient.upload(eq(fileStorageSource), any(), any(), eq(TENANT_ID), eq(USER_ID)))
            .willReturn(
                FileRs.builder()
                    .key(expectedS3Key)
                    .path(templateFolderPath)
                    .source(fileStorageSource)
                    .fileName(templateFileName)
                    .build()
            );

        // when
        String responseBody = mockMvc.perform(
                multipart("/v1/doc/template/import")
                    .file(requestPart)
                    .file(filePart)
                    .header("X-Tenant-Id", TENANT_ID)
                    .header("X-User-Id", USER_ID)
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value(templateName))
            .andExpect(jsonPath("$.code").value(templateCode))
            .andExpect(jsonPath("$.entityId").value(ENTITY_ID.toString()))
            .andExpect(jsonPath("$.active").value(true))
            .andExpect(jsonPath("$.s3Key").value(expectedS3Key))
            .andExpect(jsonPath("$.displayCondition").doesNotExist())
            .andExpect(jsonPath("$.templateMapping.length()").value(3))
            .andExpect(
                jsonPath(
                    "$.templateMapping[*].key",
                    containsInAnyOrder(
                        TemplateMappingKeys.GENERATED_FILE_NAME,
                        "deal_number",
                        "product_name"
                    )
                )
            )
            .andReturn()
            .getResponse()
            .getContentAsString();
        TemplateRs response = objectMapper.readValue(responseBody, TemplateRs.class);

        // then
        Template savedTemplate = templateService.getAggregateById(TENANT_ID, response.getId());
        assertThat(savedTemplate.getName()).isEqualTo(templateName);
        assertThat(savedTemplate.getCode()).isEqualTo(templateCode);
        assertThat(savedTemplate.getS3Key()).isEqualTo(expectedS3Key);
        assertThat(savedTemplate.isActive()).isTrue();

        List<TemplateMapping> mappings = savedTemplate.getMappings();
        assertThat(mappings).hasSize(3);
        assertThat(mappings)
            .extracting(TemplateMapping::getKey)
            .containsExactlyInAnyOrder(TemplateMappingKeys.GENERATED_FILE_NAME, "deal_number", "product_name");

        TemplateMapping generatedFileNameMapping = mappings.stream()
            .filter(mapping -> TemplateMappingKeys.GENERATED_FILE_NAME.equals(mapping.getKey()))
            .findFirst()
            .orElseThrow();
        assertThat(generatedFileNameMapping.getDefinition().getScope()).isEqualTo(MappingScope.FILE_NAME);
        assertThat(generatedFileNameMapping.getDefinition().getType()).isEqualTo(TemplateValueType.STRING);
        assertThat(generatedFileNameMapping.getDefinition().getSource()).isInstanceOf(ConstantValueSource.class);
        assertThat(((ConstantValueSource) generatedFileNameMapping.getDefinition().getSource()).getValue())
            .isEqualTo(templateName);

        List<TemplateMapping> extractedMappings = mappings.stream()
            .filter(mapping -> !TemplateMappingKeys.GENERATED_FILE_NAME.equals(mapping.getKey()))
            .sorted(Comparator.comparing(TemplateMapping::getKey))
            .toList();
        assertThat(extractedMappings.get(0).getKey()).isEqualTo("deal_number");
        assertThat(extractedMappings.get(0).getDefinition().getScope()).isEqualTo(MappingScope.VALUE);
        assertThat(extractedMappings.get(0).getDefinition().getType()).isNull();
        assertThat(extractedMappings.get(0).getDefinition().getSource()).isNull();
        assertThat(extractedMappings.get(1).getKey()).isEqualTo("product_name");
        assertThat(extractedMappings.get(1).getDefinition().getScope()).isEqualTo(MappingScope.TABLE);
        assertThat(extractedMappings.get(1).getDefinition().getType()).isNull();
        assertThat(extractedMappings.get(1).getDefinition().getSource()).isNull();

        verify(fileStorageClient).getFolder(fileStorageSource, templateFolderPath, TENANT_ID, USER_ID);
        verify(fileStorageClient).upload(eq(fileStorageSource), any(), any(), eq(TENANT_ID), eq(USER_ID));
        verifyNoMoreInteractions(fileStorageClient);
        assertThat(responseBody).contains(templateCode);
    }
}
