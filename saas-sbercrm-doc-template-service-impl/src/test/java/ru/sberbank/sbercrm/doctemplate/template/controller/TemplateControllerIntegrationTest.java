package ru.sberbank.sbercrm.doctemplate.template.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import java.nio.charset.StandardCharsets;
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
import ru.sberbank.sbercrm.doctemplate.common.CommonRqDto;
import ru.sberbank.sbercrm.doctemplate.common.CommonRsDto;
import ru.sberbank.sbercrm.doctemplate.common.FilterDto;
import ru.sberbank.sbercrm.doctemplate.common.PagingRqDto;
import ru.sberbank.sbercrm.doctemplate.common.SortTypeDto;
import ru.sberbank.sbercrm.doctemplate.template.TemplateCreationRq;
import ru.sberbank.sbercrm.doctemplate.template.TemplateRs;
import ru.sberbank.sbercrm.doctemplate.template.TemplateUpdateRq;
import ru.sberbank.sbercrm.doctemplate.template.adapter.filestorage.FileRs;
import ru.sberbank.sbercrm.doctemplate.template.adapter.filestorage.FileStorageClient;
import ru.sberbank.sbercrm.doctemplate.template.adapter.filestorage.FolderRs;
import ru.sberbank.sbercrm.doctemplate.template.constant.TemplateMappingKeys;
import ru.sberbank.sbercrm.doctemplate.template.model.MappingScope;
import ru.sberbank.sbercrm.doctemplate.template.model.Template;
import ru.sberbank.sbercrm.doctemplate.template.model.TemplateFormat;
import ru.sberbank.sbercrm.doctemplate.template.model.TemplateMapping;
import ru.sberbank.sbercrm.doctemplate.template.model.TemplateMappingDefinition;
import ru.sberbank.sbercrm.doctemplate.template.model.TemplateValueType;
import ru.sberbank.sbercrm.doctemplate.template.model.source.ConstantValueSource;
import ru.sberbank.sbercrm.doctemplate.template.TemplateMappingDefinitionDto;
import ru.sberbank.sbercrm.doctemplate.template.TemplateMappingDto;
import ru.sberbank.sbercrm.doctemplate.rule.PrimitiveRuleDto;
import ru.sberbank.sbercrm.doctemplate.template.source.ConstantValueSourceDto;
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
        String templateCode = "SUPPLY_CONTRACT_IMPORT";
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
            .andExpect(jsonPath("$.mappings.length()").value(3))
            .andExpect(
                jsonPath(
                    "$.mappings[*].key",
                    containsInAnyOrder(
                        TemplateMappingKeys.GENERATED_FILE_NAME,
                        "deal_number",
                        "product_name"
                    )
                )
            )
            .andReturn()
            .getResponse()
            .getContentAsString(StandardCharsets.UTF_8);
        TemplateRs response = objectMapper.readValue(responseBody, TemplateRs.class);

        // then
        Template savedTemplate = templateService.findAggregateById(TENANT_ID, response.getId()).orElseThrow();
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
        assertThat(response.getCode()).isEqualTo(templateCode);
    }

    @Test
    @DisplayName("Обновление шаблона заменяет метаданные и маппинги")
    void givenExistingTemplate_whenUpdateTemplate_thenPersistUpdatedTemplateAndMappings() throws Exception {
        // given
        String initialName = "Исходный шаблон";
        String initialCode = "SUPPLY_CONTRACT_UPDATE";
        String initialS3Key = "templates/" + ENTITY_ID + "/initial.docx";
        Template createdTemplate = templateService.create(
            TENANT_ID,
            Template.builder()
                .entityId(ENTITY_ID)
                .name(initialName)
                .code(initialCode)
                .description("Исходное описание")
                .format(TemplateFormat.DOCX)
                .s3Key(initialS3Key)
                .active(true)
                .createdBy(USER_ID)
                .updatedBy(USER_ID)
                .build()
        );
        templateService.createMappings(
            TENANT_ID,
            createdTemplate.getId(),
            USER_ID,
            List.of(
                TemplateMapping.builder()
                    .key("old_variable")
                    .definition(TemplateMappingDefinition.builder().scope(MappingScope.VALUE).build())
                    .build()
            )
        );

        String updatedName = "Обновленный шаблон";
        String updatedDescription = "Обновленное описание";
        String updatedGeneratedFileName = "Имя файла";
        TemplateUpdateRq request = TemplateUpdateRq.builder()
            .name(updatedName)
            .description(updatedDescription)
            .displayCondition(PrimitiveRuleDto.builder().value("true").build())
            .active(false)
            .mappings(
                List.of(
                    TemplateMappingDto.builder()
                        .key(TemplateMappingKeys.GENERATED_FILE_NAME)
                        .definition(
                            TemplateMappingDefinitionDto.builder()
                                .scope(MappingScope.FILE_NAME.value())
                                .type(TemplateValueType.STRING.value())
                                .source(ConstantValueSourceDto.builder().value(updatedGeneratedFileName).build())
                                .build()
                        )
                        .build(),
                    TemplateMappingDto.builder()
                        .key("customer_name")
                        .definition(
                            TemplateMappingDefinitionDto.builder()
                                .scope(MappingScope.VALUE.value())
                                .build()
                        )
                        .build()
                )
            )
            .build();

        // when
        String responseBody = mockMvc.perform(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/v1/doc/template/{templateId}", createdTemplate.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsBytes(request))
                    .header("X-Tenant-Id", TENANT_ID)
                    .header("X-User-Id", USER_ID)
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(createdTemplate.getId().toString()))
            .andExpect(jsonPath("$.name").value(updatedName))
            .andExpect(jsonPath("$.description").value(updatedDescription))
            .andExpect(jsonPath("$.code").value(initialCode))
            .andExpect(jsonPath("$.s3Key").value(initialS3Key))
            .andExpect(jsonPath("$.active").value(false))
            .andExpect(jsonPath("$.mappings.length()").value(2))
            .andReturn()
            .getResponse()
            .getContentAsString(StandardCharsets.UTF_8);
        TemplateRs response = objectMapper.readValue(responseBody, TemplateRs.class);

        // then
        Template updatedTemplate = templateService.findAggregateById(TENANT_ID, response.getId()).orElseThrow();
        assertThat(updatedTemplate.getName()).isEqualTo(updatedName);
        assertThat(updatedTemplate.getDescription()).isEqualTo(updatedDescription);
        assertThat(updatedTemplate.getCode()).isEqualTo(initialCode);
        assertThat(updatedTemplate.getS3Key()).isEqualTo(initialS3Key);
        assertThat(updatedTemplate.getEntityId()).isEqualTo(ENTITY_ID);
        assertThat(updatedTemplate.isActive()).isFalse();
        assertThat(updatedTemplate.getDisplayCondition()).isNotNull();

        List<TemplateMapping> mappings = updatedTemplate.getMappings();
        assertThat(mappings).hasSize(2);
        assertThat(mappings)
            .extracting(TemplateMapping::getKey)
            .containsExactlyInAnyOrder(TemplateMappingKeys.GENERATED_FILE_NAME, "customer_name");
        assertThat(mappings)
            .extracting(TemplateMapping::getKey)
            .doesNotContain("old_variable");

        TemplateMapping generatedFileNameMapping = mappings.stream()
            .filter(mapping -> TemplateMappingKeys.GENERATED_FILE_NAME.equals(mapping.getKey()))
            .findFirst()
            .orElseThrow();
        assertThat(generatedFileNameMapping.getDefinition().getScope()).isEqualTo(MappingScope.FILE_NAME);
        assertThat(generatedFileNameMapping.getDefinition().getType()).isEqualTo(TemplateValueType.STRING);
        assertThat(generatedFileNameMapping.getDefinition().getSource()).isInstanceOf(ConstantValueSource.class);
        assertThat(((ConstantValueSource) generatedFileNameMapping.getDefinition().getSource()).getValue())
            .isEqualTo(updatedGeneratedFileName);

        assertThat(response.getName()).isEqualTo(updatedName);
        verifyNoMoreInteractions(fileStorageClient);
    }

    @Test
    @DisplayName("Удаление шаблона удаляет файл и записи шаблона")
    void givenExistingTemplate_whenDeleteTemplate_thenDeleteTemplateAndFile() throws Exception {
        // given
        String templateName = "Шаблон на удаление";
        String templateCode = "SUPPLY_CONTRACT_DELETE";
        String templateS3Key = "templates/" + ENTITY_ID + "/delete.docx";
        Template createdTemplate = templateService.create(
            TENANT_ID,
            Template.builder()
                .entityId(ENTITY_ID)
                .name(templateName)
                .code(templateCode)
                .description("Шаблон для удаления")
                .format(TemplateFormat.DOCX)
                .s3Key(templateS3Key)
                .active(true)
                .createdBy(USER_ID)
                .updatedBy(USER_ID)
                .build()
        );
        templateService.createMappings(
            TENANT_ID,
            createdTemplate.getId(),
            USER_ID,
            List.of(
                TemplateMapping.builder()
                    .key("delete_variable")
                    .definition(TemplateMappingDefinition.builder().scope(MappingScope.VALUE).build())
                    .build()
            )
        );
        String fileStorageSource = "doc-template-service";

        // when
        mockMvc.perform(
                delete("/v1/doc/template/{templateId}", createdTemplate.getId())
                    .header("X-Tenant-Id", TENANT_ID)
                    .header("X-User-Id", USER_ID)
            )
            .andExpect(status().isNoContent());

        // then
        assertThat(templateService.findAggregateById(TENANT_ID, createdTemplate.getId())).isEmpty();
        verify(fileStorageClient).deleteFile(fileStorageSource, templateS3Key, TENANT_ID, USER_ID);
        verifyNoMoreInteractions(fileStorageClient);
    }

    @Test
    @DisplayName("Список шаблонов возвращает отфильтрованные и отсортированные шаблоны")
    void givenTemplates_whenListTemplates_thenReturnFilteredAndSortedTemplates() throws Exception {
        // given
        UUID listEntityId = UUID.fromString("44444444-4444-4444-4444-444444444444");
        Template alphaTemplate = templateService.create(
            TENANT_ID,
            Template.builder()
                .entityId(listEntityId)
                .name("Альфа")
                .code("LIST_ALPHA")
                .description("Первый")
                .format(TemplateFormat.DOCX)
                .s3Key("templates/" + ENTITY_ID + "/alpha.docx")
                .active(true)
                .createdBy(USER_ID)
                .updatedBy(USER_ID)
                .build()
        );
        Template betaTemplate = templateService.create(
            TENANT_ID,
            Template.builder()
                .entityId(listEntityId)
                .name("Бета")
                .code("LIST_BETA")
                .description("Второй")
                .format(TemplateFormat.DOCX)
                .s3Key("templates/" + ENTITY_ID + "/beta.docx")
                .active(true)
                .createdBy(USER_ID)
                .updatedBy(USER_ID)
                .build()
        );
        templateService.create(
            TENANT_ID,
            Template.builder()
                .entityId(listEntityId)
                .name("Гамма")
                .code("LIST_GAMMA")
                .description("Третий")
                .format(TemplateFormat.DOCX)
                .s3Key("templates/" + ENTITY_ID + "/gamma.docx")
                .active(false)
                .createdBy(USER_ID)
                .updatedBy(USER_ID)
                .build()
        );

        CommonRqDto request = CommonRqDto.builder()
            .paging(PagingRqDto.builder().page(0).size(2).build())
            .sort(List.of(SortTypeDto.builder().field("name").direction(SortTypeDto.Direction.DESC).build()))
            .filter(
                java.util.Set.of(
                    FilterDto.builder()
                        .field("active")
                        .operation(FilterDto.Operation.EQUAL)
                        .value(List.of(true))
                        .build(),
                    FilterDto.builder()
                        .field("entityId")
                        .operation(FilterDto.Operation.EQUAL)
                        .value(List.of(listEntityId))
                        .build()
                )
            )
            .build();

        // when
        String responseBody = mockMvc.perform(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/v1/doc/template/list")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsBytes(request))
                    .header("X-Tenant-Id", TENANT_ID)
                    .header("X-User-Id", USER_ID)
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(2))
            .andExpect(jsonPath("$.data[0].id").value(betaTemplate.getId().toString()))
            .andExpect(jsonPath("$.data[0].name").value("Бета"))
            .andExpect(jsonPath("$.data[1].id").value(alphaTemplate.getId().toString()))
            .andExpect(jsonPath("$.data[1].name").value("Альфа"))
            .andExpect(jsonPath("$.paging.currentPage").value(0))
            .andExpect(jsonPath("$.paging.recordsOnPage").value(2))
            .andExpect(jsonPath("$.paging.totalRecordsAmount").value(2))
            .andExpect(jsonPath("$.paging.totalPageAmount").value(1))
            .andReturn()
            .getResponse()
            .getContentAsString(StandardCharsets.UTF_8);
        CommonRsDto response = objectMapper.readValue(responseBody, CommonRsDto.class);
        @SuppressWarnings("unchecked")
        List<java.util.Map<String, Object>> data = (List<java.util.Map<String, Object>>) response.getData();

        // then
        assertThat(data).hasSize(2);
        assertThat(data.get(0).get("id")).isEqualTo(betaTemplate.getId().toString());
        assertThat(data.get(1).get("id")).isEqualTo(alphaTemplate.getId().toString());
        verifyNoMoreInteractions(fileStorageClient);
    }
}
