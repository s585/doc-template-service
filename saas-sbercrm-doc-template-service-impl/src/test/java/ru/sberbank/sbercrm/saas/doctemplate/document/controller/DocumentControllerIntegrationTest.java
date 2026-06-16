package ru.sberbank.sbercrm.saas.doctemplate.document.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFTable;
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
import ru.sberbank.sbercrm.saas.doctemplate.document.usecase.GenerationJobExecutionUseCase;
import ru.sberbank.sbercrm.saas.doctemplate.shared.dto.CommonRqDto;
import ru.sberbank.sbercrm.saas.doctemplate.shared.dto.FilterDto;
import ru.sberbank.sbercrm.saas.doctemplate.shared.dto.PagingRqDto;
import ru.sberbank.sbercrm.saas.doctemplate.shared.dto.SelectDto;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.Template;

@TestPropertySource(properties = {
    "saas.doc-template.file-storage.local.enabled=true",
    "saas.doc-template.generation.scheduler-enabled=false",
    "saas.doc-template.generation.retry-backoff-seconds=0,0,0,0"
})
class DocumentControllerIntegrationTest extends AbstractDocumentGenerationIntegrationTest {
    private static final UUID OBJECT_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID REQUEST_ID_DONE = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final UUID REQUEST_ID_REFERENCE = UUID.fromString("66666666-6666-6666-6666-666666666666");
    private static final UUID REQUEST_ID_ERROR = UUID.fromString("77777777-7777-7777-7777-777777777777");
    private static final UUID REQUEST_ID_REFERENCE_TABLE = UUID.fromString("12121212-1212-1212-1212-121212121212");
    private static final UUID REQUEST_ID_MISSING_REFERENCE = UUID.fromString("13131313-1313-1313-1313-131313131313");
    private static final UUID TEMPLATE_SUFFIX_DONE = UUID.fromString("88888888-8888-8888-8888-888888888888");
    private static final UUID TEMPLATE_SUFFIX_ERROR = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID TEMPLATE_SUFFIX_MISSING_REFERENCE = UUID.fromString("14141414-1414-1414-1414-141414141414");
    private static final UUID WORKER_ID_DONE = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID WORKER_ID_ERROR = UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee");
    private static final UUID WORKER_ID_ERROR_CLAIM_CHECK = UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff");
    private static final UUID WORKER_ID_MISSING_REFERENCE = UUID.fromString("15151515-1515-1515-1515-151515151515");
    private static final UUID REFERENCE_ENTITY_ID = UUID.fromString("99999999-9999-9999-9999-999999999999");

    @Autowired
    private GenerationJobExecutionUseCase generationJobExecutionUseCase;

    @Autowired
    private GenerationJobService generationJobService;

    @Autowired
    private GenerationJobAttemptService generationJobAttemptService;

    @Test
    @DisplayName("Генерация документа резолвит переменные разных типов и создает итоговый DOCX")
    void givenMixedMappings_whenGenerateDocument_thenPersistDoneFileAndResolvedContent() throws Exception {
        String templateKey = "templates/" + ENTITY_ID + "/generation-" + TEMPLATE_SUFFIX_DONE + ".docx";
        writeTemplateToStubStorage(templateKey, createDocx("Contract for ${customer_name} [${customer_type}]"));
        businessObjectWireMock.stubGetObjectWithSpecifiedFields(
            TENANT_ID,
            USER_ID,
            ENTITY_ID,
            OBJECT_ID,
            SelectDto.builder().fields(java.util.Set.of("customer.name")).build(),
            Map.of(
                "customer",
                Map.of("name", "Business Object LLC")
            )
        );

        Template template = createDocxTemplateWithMappings(
            templateKey,
            "Шаблон договора mixed",
            "DOC_GENERATION_" + TEMPLATE_SUFFIX_DONE,
            "Описание шаблона mixed",
            List.of(
                buildGeneratedFileNameMapping("ready-contract"),
                buildDirectValueMapping("customer_name", "source.customer.name"),
                buildConstantValueMapping("customer_type", "VIP")
            )
        );

        DocumentRs createdDocument = createDocument(template.getId(), OBJECT_ID, REQUEST_ID_REFERENCE);
        assertThat(createdDocument.getTemplateId()).isEqualTo(template.getId());
        assertThat(createdDocument.getFiles()).hasSize(1);
        assertThat(createdDocument.getFiles().getFirst().getStatus())
            .isEqualTo(GeneratedFileStatus.PENDING.name());

        var claimedJob = generationJobService.claimNextJobs(WORKER_ID_DONE, 1).getFirst();
        generationJobExecutionUseCase.execute(claimedJob);

        DocumentRs generatedDocument = getDocument(createdDocument.getId());

        assertThat(generatedDocument.getId()).isEqualTo(createdDocument.getId());
        assertThat(generatedDocument.getFiles()).hasSize(1);
        assertThat(generatedDocument.getFiles().getFirst().getStatus())
            .isEqualTo(GeneratedFileStatus.DONE.name());
        assertThat(generatedDocument.getFiles().getFirst().getS3Key()).isNotBlank();
        assertThat(generatedDocument.getFiles().getFirst().getChecksum()).isNotBlank();
        assertThat(generatedDocument.getFiles().getFirst().getSizeBytes()).isPositive();
        String generatedKey = generatedDocument.getFiles().getFirst().getS3Key();
        assertThat(generatedKey)
            .startsWith(
                "documents/"
                    + ENTITY_ID
                    + "/"
                    + OBJECT_ID
                    + "/"
                    + createdDocument.getId()
                    + "/"
            )
            .endsWith("_ready-contract.docx");

        byte[] generatedFile = fileStorageGateway.download(TENANT_ID, USER_ID, generatedKey);
        String generatedText = readDocxText(generatedFile);

        assertThat(generatedText).contains("Contract for Business Object LLC [VIP]");
        assertThat(generatedText).doesNotContain("${customer_name}");
        assertThat(generatedText).doesNotContain("${customer_type}");
        businessObjectWireMock.verifyGetObjectWithSpecifiedFields(
            TENANT_ID,
            USER_ID,
            ENTITY_ID,
            OBJECT_ID,
            SelectDto.builder().fields(Set.of("customer.name")).build()
        );
        assertThat(generationJobAttemptService.findByJobId(claimedJob.getId()))
            .singleElement()
            .satisfies(attempt -> {
                assertThat(attempt.getAttemptNo()).isEqualTo(1);
                assertThat(attempt.getStatus()).isEqualTo(GenerationJobAttemptStatus.DONE.name());
                assertThat(attempt.getWorkerId()).isNotNull();
                assertThat(attempt.getFinishedAt()).isNotNull();
            });
    }

    @Test
    @DisplayName("Генерация документа резолвит REFERENCE mapping через list-objects")
    void givenReferenceMapping_whenGenerateDocument_thenUseReferenceLookup() throws Exception {
        String templateKey = "templates/" + ENTITY_ID + "/reference-" + TEMPLATE_SUFFIX_DONE + ".docx";
        writeTemplateToStubStorage(templateKey, createDocxListItem("Contract product: ${product_name}"));
        SelectDto selectDto = SelectDto.builder()
            .fields(java.util.Set.of("document$c.id", "document$c.dealProduct$c"))
            .build();
        businessObjectWireMock.stubGetObjectWithSpecifiedFields(
            TENANT_ID,
            USER_ID,
            ENTITY_ID,
            OBJECT_ID,
            selectDto,
            Map.of(
                "document$c",
                Map.of(
                    "id", "doc-1",
                    "dealProduct$c", List.of()
                )
            )
        );
        CommonRqDto listRequest = CommonRqDto.builder()
            .select(SelectDto.builder().fields(Set.of("product.name")).build())
            .filter(
                Set.of(
                    FilterDto.builder()
                        .field("document$c")
                        .operation(FilterDto.Operation.EQUAL)
                        .value(List.of("doc-1"))
                        .build()
                )
            )
            .sort(List.of())
            .paging(PagingRqDto.builder().page(0).size(100).build())
            .build();
        businessObjectWireMock.stubListObjects(
            TENANT_ID,
            USER_ID,
            REFERENCE_ENTITY_ID,
            listRequest,
            Map.of(
                "data",
                List.of(
                    Map.of("product", Map.of("name", "Product from reference"))
                ),
                "errors", List.of(),
                "commonErrors", List.of(),
                "paging", Map.of("currentPage", 0, "recordsOnPage", 1)
            )
        );

        Template template = createDocxTemplateWithMappings(
            templateKey,
            "Шаблон reference",
            "DOC_REFERENCE_" + TEMPLATE_SUFFIX_DONE,
            "Описание reference шаблона",
            List.of(
                buildGeneratedFileNameMapping("reference-contract"),
                buildReferenceValueMapping(
                    "product_name",
                    REFERENCE_ENTITY_ID,
                    "source.document$c.dealProduct$c",
                    "document$c",
                    "source.document$c.id",
                    "reference.product.name"
                )
            )
        );

        DocumentRs createdDocument = createDocument(template.getId(), OBJECT_ID, REQUEST_ID_REFERENCE_TABLE);

        var claimedJob = generationJobService.claimNextJobs(WORKER_ID_DONE, 1).getFirst();
        generationJobExecutionUseCase.execute(claimedJob);

        DocumentRs generatedDocument = getDocument(createdDocument.getId());
        byte[] generatedFile = fileStorageGateway.download(TENANT_ID, USER_ID, generatedDocument.getFiles().getFirst().getS3Key());
        String generatedText = readDocxText(generatedFile);

        assertThat(generatedText).contains("Contract product: Product from reference");
    }

    @Test
    @DisplayName("Генерация документа размножает строки таблицы для reference collection")
    void givenReferenceCollectionTable_whenGenerateDocument_thenRepeatTemplateRows() throws Exception {
        String templateKey = "templates/" + ENTITY_ID + "/reference-table-" + TEMPLATE_SUFFIX_DONE + ".docx";
        writeTemplateToStubStorage(
            templateKey,
            createDocxTable(
                List.of("Product", "Qty"),
                List.of("${product_name}", "${product_qty}")
            )
        );

        SelectDto selectDto = SelectDto.builder()
            .fields(Set.of("document$c.id", "document$c.dealProduct$c"))
            .build();
        businessObjectWireMock.stubGetObjectWithSpecifiedFields(
            TENANT_ID,
            USER_ID,
            ENTITY_ID,
            OBJECT_ID,
            selectDto,
            Map.of(
                "document$c",
                Map.of(
                    "id", "doc-table-1",
                    "dealProduct$c", List.of()
                )
            )
        );
        businessObjectWireMock.stubListObjects(
            TENANT_ID,
            USER_ID,
            REFERENCE_ENTITY_ID,
            CommonRqDto.builder()
                .select(SelectDto.builder().fields(Set.of("product.name", "quantity")).build())
                .filter(
                    Set.of(
                        FilterDto.builder()
                            .field("document$c")
                            .operation(FilterDto.Operation.EQUAL)
                            .value(List.of("doc-table-1"))
                            .build()
                    )
                )
                .sort(List.of())
                .paging(PagingRqDto.builder().page(0).size(100).build())
                .build(),
            Map.of(
                "data",
                List.of(
                    Map.of("product", Map.of("name", "Product A"), "quantity", 2),
                    Map.of("product", Map.of("name", "Product B"), "quantity", 1)
                ),
                "errors", List.of(),
                "commonErrors", List.of(),
                "paging", Map.of("currentPage", 0, "recordsOnPage", 2)
            )
        );

        Template template = createDocxTemplateWithMappings(
            templateKey,
            "Шаблон reference table",
            "DOC_REFERENCE_TABLE_" + TEMPLATE_SUFFIX_DONE,
            "Описание reference table шаблона",
            List.of(
                buildGeneratedFileNameMapping("reference-table-contract"),
                buildReferenceValueMapping(
                    "product_name",
                    REFERENCE_ENTITY_ID,
                    "source.document$c.dealProduct$c",
                    "document$c",
                    "source.document$c.id",
                    "reference.product.name"
                ),
                buildReferenceValueMapping(
                    "product_qty",
                    REFERENCE_ENTITY_ID,
                    "source.document$c.dealProduct$c",
                    "document$c",
                    "source.document$c.id",
                    "reference.quantity"
                )
            )
        );

        DocumentRs createdDocument = createDocument(template.getId(), OBJECT_ID, REQUEST_ID_DONE);

        var claimedJob = generationJobService.claimNextJobs(WORKER_ID_DONE, 1).getFirst();
        generationJobExecutionUseCase.execute(claimedJob);

        DocumentRs generatedDocument = getDocument(createdDocument.getId());
        byte[] generatedFile = fileStorageGateway.download(TENANT_ID, USER_ID, generatedDocument.getFiles().getFirst().getS3Key());

        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(generatedFile))) {
            XWPFTable table = document.getTables().getFirst();
            assertThat(table.getRows()).hasSize(3);
            assertThat(table.getRow(1).getCell(0).getText()).isEqualTo("Product A");
            assertThat(table.getRow(1).getCell(1).getText()).isEqualTo("2");
            assertThat(table.getRow(2).getCell(0).getText()).isEqualTo("Product B");
            assertThat(table.getRow(2).getCell(1).getText()).isEqualTo("1");
        }
    }

    @Test
    @DisplayName("Отсутствующее значение по reference path подставляется пустой строкой")
    void givenMissingReferencePathValue_whenExecuteThenSubstituteEmptyString() throws Exception {
        String templateKey = "templates/" + ENTITY_ID + "/missing-reference-" + TEMPLATE_SUFFIX_MISSING_REFERENCE + ".docx";
        writeTemplateToStubStorage(templateKey, createDocxListItem("Contract for ${customer_name}"));
        SelectDto selectDto = SelectDto.builder()
            .fields(java.util.Set.of("document$c.id", "document$c.dealProduct$c"))
            .build();
        businessObjectWireMock.stubGetObjectWithSpecifiedFields(
            TENANT_ID,
            USER_ID,
            ENTITY_ID,
            OBJECT_ID,
            selectDto,
            Map.of(
                "document$c",
                Map.of(
                    "id", "doc-missing-reference",
                    "dealProduct$c", List.of()
                )
            )
        );
        businessObjectWireMock.stubListObjects(
            TENANT_ID,
            USER_ID,
            REFERENCE_ENTITY_ID,
            CommonRqDto.builder()
                .select(SelectDto.builder().fields(Set.of("customer.name")).build())
                .filter(java.util.Set.of(
                    FilterDto.builder()
                        .field("document$c")
                        .operation(FilterDto.Operation.EQUAL)
                        .value(List.of("doc-missing-reference"))
                        .build()
                ))
                .sort(List.of())
                .paging(PagingRqDto.builder().page(0).size(100).build())
                .build(),
            Map.of(
                "data",
                List.of(Map.of("customer", Map.of())),
                "errors",
                List.of(),
                "commonErrors",
                List.of(),
                "paging",
                Map.of("currentPage", 0, "recordsOnPage", 1)
            )
        );

        Template template = createDocxTemplateWithMappings(
            templateKey,
            "Шаблон с отсутствующим reference значением",
            "DOC_MISSING_REFERENCE_" + TEMPLATE_SUFFIX_MISSING_REFERENCE,
            "Описание шаблона с отсутствующим reference значением",
            List.of(
                buildReferenceValueMapping(
                    "customer_name",
                    REFERENCE_ENTITY_ID,
                    "source.document$c.dealProduct$c",
                    "document$c",
                    "source.document$c.id",
                    "reference.customer.name"
                )
            )
        );

        DocumentRs createdDocument = createDocument(template.getId(), OBJECT_ID, REQUEST_ID_MISSING_REFERENCE);

        var claimedJob = generationJobService.claimNextJobs(WORKER_ID_MISSING_REFERENCE, 1).getFirst();
        generationJobExecutionUseCase.execute(claimedJob);

        DocumentRs generatedDocument = getDocument(createdDocument.getId());
        assertThat(generatedDocument.getFiles().getFirst().getStatus()).isEqualTo(GeneratedFileStatus.DONE.name());

        byte[] generatedFile = fileStorageGateway.download(
            TENANT_ID,
            USER_ID,
            generatedDocument.getFiles().getFirst().getS3Key()
        );
        String generatedText = readDocxText(generatedFile);
        assertThat(generatedText).contains("Contract for");
        assertThat(generatedText).doesNotContain("${customer_name}");
    }

    @Test
    @DisplayName("Детерминированная ошибка в синтаксически невалидном reference path завершает файл и job в ERROR без повторной попытки")
    void givenInvalidReferencePath_whenExecuteThenFailWithoutRetry() throws Exception {
        String templateKey = "templates/" + ENTITY_ID + "/non-retry-" + TEMPLATE_SUFFIX_ERROR + ".docx";
        writeTemplateToStubStorage(templateKey, createDocxListItem("Contract for ${customer_name}"));
        SelectDto selectDto = SelectDto.builder()
            .fields(java.util.Set.of("document$c.id", "document$c.dealProduct$c"))
            .build();
        businessObjectWireMock.stubGetObjectWithSpecifiedFields(
            TENANT_ID,
            USER_ID,
            ENTITY_ID,
            OBJECT_ID,
            selectDto,
            Map.of(
                "document$c", Map.of(
                    "id", "doc-2",
                    "dealProduct$c", List.of()
                )
            )
        );
        Template template = createDocxTemplateWithMappings(
            templateKey,
            "Шаблон без retry",
            "DOC_NON_RETRY_" + TEMPLATE_SUFFIX_ERROR,
            "Описание шаблона без retry",
            List.of(
                buildReferenceValueMapping(
                    "customer_name",
                    REFERENCE_ENTITY_ID,
                    "source.document$c.dealProduct$c",
                    "document$c",
                    "source.document$c.id",
                    "reference."
                )
            )
        );

        DocumentRs createdDocument = createDocument(template.getId(), OBJECT_ID, REQUEST_ID_ERROR);

        var claimedJob = generationJobService.claimNextJobs(WORKER_ID_ERROR, 1).getFirst();
        generationJobExecutionUseCase.execute(claimedJob);

        mockMvc.perform(
                get("/v1/doc/{documentId}", createdDocument.getId())
                    .header("X-Tenant-Id", TENANT_ID)
                    .header("X-User-Id", USER_ID)
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.files[0].status").value(GeneratedFileStatus.ERROR.name()))
            .andExpect(jsonPath("$.files[0].errorCode").value("generation.business_object_path_invalid"))
            .andExpect(
                jsonPath("$.files[0].errorMessage")
                    .value("Invalid business object path for mapping: key=reference-select, path=reference.")
            );

        assertThat(generationJobService.findById(TENANT_ID, claimedJob.getId()))
            .get()
            .satisfies(job -> {
                assertThat(job.getStatus()).isEqualTo(GenerationJobStatus.ERROR.name());
                assertThat(job.getAttemptCount()).isEqualTo(1);
                assertThat(job.getNextRetryAt()).isNull();
                assertThat(job.getErrorCode()).isEqualTo("generation.business_object_path_invalid");
            });

        assertThat(generationJobAttemptService.findByJobId(claimedJob.getId()))
            .singleElement()
            .satisfies(attempt -> {
                assertThat(attempt.getAttemptNo()).isEqualTo(1);
                assertThat(attempt.getStatus()).isEqualTo(GenerationJobAttemptStatus.ERROR.name());
                assertThat(attempt.getErrorCode()).isEqualTo("generation.business_object_path_invalid");
            });

        assertThat(generationJobService.claimNextJobs(WORKER_ID_ERROR_CLAIM_CHECK, 1))
            .noneMatch(job -> job.getId().equals(claimedJob.getId()));
    }

    private DocumentRs createDocument(UUID templateId, UUID requestId) throws Exception {
        return createDocument(templateId, OBJECT_ID, requestId);
    }
}
