package ru.sberbank.sbercrm.saas.doctemplate.template.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import ru.sberbank.sbercrm.doctemplate.shared.dto.CommonRqDto;
import ru.sberbank.sbercrm.doctemplate.shared.dto.CommonRsDto;
import ru.sberbank.sbercrm.doctemplate.shared.dto.FilterDto;
import ru.sberbank.sbercrm.doctemplate.shared.dto.PagingRqDto;
import ru.sberbank.sbercrm.doctemplate.shared.dto.SortTypeDto;
import ru.sberbank.sbercrm.doctemplate.template.dto.TemplateCreationRq;
import ru.sberbank.sbercrm.doctemplate.template.dto.TemplateRs;
import ru.sberbank.sbercrm.doctemplate.template.dto.TemplateUpdateRq;
import ru.sberbank.sbercrm.saas.doctemplate.AbstractIntegrationTest;
import ru.sberbank.sbercrm.saas.doctemplate.template.constant.TemplateConstants;
import ru.sberbank.sbercrm.saas.doctemplate.template.gateway.filestorage.FileStorageWireMock;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.MappingScope;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.Template;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.TemplateFormat;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.TemplateMapping;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.TemplateMappingDefinition;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.TemplateValueType;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.source.ConstantValueSource;
import ru.sberbank.sbercrm.doctemplate.template.dto.TemplateMappingDefinitionDto;
import ru.sberbank.sbercrm.doctemplate.template.dto.TemplateMappingDto;
import ru.sberbank.sbercrm.doctemplate.template.dto.source.ConstantValueSourceDto;

class TemplateControllerIntegrationTest extends AbstractIntegrationTest {

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
        FileStorageWireMock.stubUploadFile(
            objectMapper,
            TENANT_ID,
            USER_ID,
            fileStorageSource,
            templateFolderPath,
            templateFileName,
            expectedS3Key
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
                        TemplateConstants.MappingKeys.GENERATED_FILE_NAME,
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
        Template savedTemplate = templateMother.findAggregateById(TENANT_ID, response.getId());
        assertThat(savedTemplate.getName()).isEqualTo(templateName);
        assertThat(savedTemplate.getCode()).isEqualTo(templateCode);
        assertThat(savedTemplate.getS3Key()).isEqualTo(expectedS3Key);
        assertThat(savedTemplate.isActive()).isTrue();

        List<TemplateMapping> mappings = savedTemplate.getMappings();
        assertThat(mappings).hasSize(3);
        assertThat(mappings)
            .extracting(TemplateMapping::getKey)
            .containsExactlyInAnyOrder(TemplateConstants.MappingKeys.GENERATED_FILE_NAME, "deal_number", "product_name");

        TemplateMapping generatedFileNameMapping = mappings.stream()
            .filter(mapping -> TemplateConstants.MappingKeys.GENERATED_FILE_NAME.equals(mapping.getKey()))
            .findFirst()
            .orElseThrow();
        assertThat(generatedFileNameMapping.getDefinition().getScope()).isEqualTo(MappingScope.FILE_NAME);
        assertThat(generatedFileNameMapping.getDefinition().getType()).isEqualTo(TemplateValueType.STRING);
        assertThat(generatedFileNameMapping.getDefinition().getSource()).isInstanceOf(ConstantValueSource.class);
        assertThat(((ConstantValueSource) generatedFileNameMapping.getDefinition().getSource()).getValue())
            .isEqualTo(templateName);

        List<TemplateMapping> extractedMappings = mappings.stream()
            .filter(mapping -> !TemplateConstants.MappingKeys.GENERATED_FILE_NAME.equals(mapping.getKey()))
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

        FileStorageWireMock.verifyUploadFile(TENANT_ID, USER_ID, fileStorageSource);
        assertThat(response.getCode()).isEqualTo(templateCode);
    }

    @Test
    @DisplayName("Обновление шаблона заменяет метаданные и маппинги")
    void givenExistingTemplate_whenUpdateTemplate_thenPersistUpdatedTemplateAndMappings() throws Exception {
        // given
        String initialName = "Исходный шаблон";
        String initialCode = "SUPPLY_CONTRACT_UPDATE";
        String initialS3Key = "templates/" + ENTITY_ID + "/initial.docx";
        Template createdTemplate = templateMother.createTemplateWithMappings(
            TENANT_ID,
            USER_ID,
            ENTITY_ID,
            initialName,
            initialCode,
            "Исходное описание",
            TemplateFormat.DOCX,
            initialS3Key,
            true,
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
            .displayCondition(FilterDto.builder().operation(FilterDto.Operation.TRUE).build())
            .active(false)
            .mappings(
                List.of(
                    TemplateMappingDto.builder()
                        .key(TemplateConstants.MappingKeys.GENERATED_FILE_NAME)
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
        Template updatedTemplate = templateMother.findAggregateById(TENANT_ID, response.getId());
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
            .containsExactlyInAnyOrder(TemplateConstants.MappingKeys.GENERATED_FILE_NAME, "customer_name");
        assertThat(mappings)
            .extracting(TemplateMapping::getKey)
            .doesNotContain("old_variable");

        TemplateMapping generatedFileNameMapping = mappings.stream()
            .filter(mapping -> TemplateConstants.MappingKeys.GENERATED_FILE_NAME.equals(mapping.getKey()))
            .findFirst()
            .orElseThrow();
        assertThat(generatedFileNameMapping.getDefinition().getScope()).isEqualTo(MappingScope.FILE_NAME);
        assertThat(generatedFileNameMapping.getDefinition().getType()).isEqualTo(TemplateValueType.STRING);
        assertThat(generatedFileNameMapping.getDefinition().getSource()).isInstanceOf(ConstantValueSource.class);
        assertThat(((ConstantValueSource) generatedFileNameMapping.getDefinition().getSource()).getValue())
            .isEqualTo(updatedGeneratedFileName);

        assertThat(response.getName()).isEqualTo(updatedName);
    }

    @Test
    @DisplayName("Удаление шаблона удаляет файл и записи шаблона")
    void givenExistingTemplate_whenDeleteTemplate_thenDeleteTemplateAndFile() throws Exception {
        // given
        String templateName = "Шаблон на удаление";
        String templateCode = "SUPPLY_CONTRACT_DELETE";
        String templateS3Key = "templates/" + ENTITY_ID + "/delete.docx";
        Template createdTemplate = templateMother.createTemplateWithMappings(
            TENANT_ID,
            USER_ID,
            ENTITY_ID,
            templateName,
            templateCode,
            "Шаблон для удаления",
            TemplateFormat.DOCX,
            templateS3Key,
            true,
            List.of(
                TemplateMapping.builder()
                    .key("delete_variable")
                    .definition(TemplateMappingDefinition.builder().scope(MappingScope.VALUE).build())
                    .build()
            )
        );
        String fileStorageSource = "doc-template-service";
        FileStorageWireMock.stubDeleteFile(TENANT_ID, USER_ID, fileStorageSource, templateS3Key);

        // when
        mockMvc.perform(
                delete("/v1/doc/template/{templateId}", createdTemplate.getId())
                    .header("X-Tenant-Id", TENANT_ID)
                    .header("X-User-Id", USER_ID)
            )
            .andExpect(status().isNoContent());

        // then
        assertThat(templateMother.exists(TENANT_ID, createdTemplate.getId())).isFalse();
        FileStorageWireMock.verifyDeleteFile(TENANT_ID, USER_ID, fileStorageSource, templateS3Key);
    }

    @Test
    @DisplayName("Список шаблонов возвращает отфильтрованные и отсортированные шаблоны")
    void givenTemplates_whenListTemplates_thenReturnFilteredAndSortedTemplates() throws Exception {
        // given
        UUID listEntityId = UUID.fromString("44444444-4444-4444-4444-444444444444");
        Template alphaTemplate = templateMother.createTemplate(
            TENANT_ID,
            USER_ID,
            listEntityId,
            "Альфа",
            "LIST_ALPHA",
            "Первый",
            TemplateFormat.DOCX,
            "templates/" + ENTITY_ID + "/alpha.docx",
            true
        );
        Template betaTemplate = templateMother.createTemplate(
            TENANT_ID,
            USER_ID,
            listEntityId,
            "Бета",
            "LIST_BETA",
            "Второй",
            TemplateFormat.DOCX,
            "templates/" + ENTITY_ID + "/beta.docx",
            true
        );
        templateMother.createTemplate(
            TENANT_ID,
            USER_ID,
            listEntityId,
            "Гамма",
            "LIST_GAMMA",
            "Третий",
            TemplateFormat.DOCX,
            "templates/" + ENTITY_ID + "/gamma.docx",
            false
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
        assertThat(data.get(0)).containsEntry("id", betaTemplate.getId().toString());
        assertThat(data.get(1)).containsEntry("id", alphaTemplate.getId().toString());
    }
}
