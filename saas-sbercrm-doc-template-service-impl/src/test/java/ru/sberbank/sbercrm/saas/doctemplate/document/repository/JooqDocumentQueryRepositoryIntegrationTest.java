package ru.sberbank.sbercrm.saas.doctemplate.document.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.sberbank.sbercrm.saas.doctemplate.jooq.tables.TGeneratedDocument.T_GENERATED_DOCUMENT;
import static ru.sberbank.sbercrm.saas.doctemplate.jooq.tables.TGeneratedFile.T_GENERATED_FILE;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import ru.sberbank.sbercrm.saas.doctemplate.AbstractIntegrationTest;
import ru.sberbank.sbercrm.saas.doctemplate.document.constant.DocumentConstants;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.Document;
import ru.sberbank.sbercrm.saas.doctemplate.shared.dto.CommonRqDto;
import ru.sberbank.sbercrm.saas.doctemplate.shared.dto.FilterDto;
import ru.sberbank.sbercrm.saas.doctemplate.shared.dto.PagingRqDto;
import ru.sberbank.sbercrm.saas.doctemplate.shared.dto.SortTypeDto;

@TestPropertySource(properties = "saas.doc-template.generation.enabled=false")
class JooqDocumentQueryRepositoryIntegrationTest extends AbstractIntegrationTest {
    private static final UUID TEMPLATE_ID = UUID.fromString("31313131-3131-3131-3131-313131313131");
    private static final UUID FIRST_DOCUMENT_ID = UUID.fromString("32323232-3232-3232-3232-323232323232");
    private static final UUID SECOND_DOCUMENT_ID = UUID.fromString("33333333-4444-5555-6666-777777777777");
    private static final UUID THIRD_DOCUMENT_ID = UUID.fromString("34343434-3434-3434-3434-343434343434");
    private static final UUID OTHER_OBJECT_ID = UUID.fromString("35353535-3535-3535-3535-353535353535");
    private static final UUID REQUEST_ID_ONE = UUID.fromString("36363636-3636-3636-3636-363636363636");
    private static final UUID REQUEST_ID_TWO = UUID.fromString("37373737-3737-3737-3737-373737373737");
    private static final UUID REQUEST_ID_THREE = UUID.fromString("38383838-3838-3838-3838-383838383838");
    private static final UUID DOCX_FILE_ID = UUID.fromString("42424242-4242-4242-4242-424242424242");
    private static final UUID XLSX_FILE_ID = UUID.fromString("43434343-4343-4343-4343-434343434343");
    private static final UUID PDF_FILE_ID = UUID.fromString("44444444-5555-6666-7777-888888888888");

    @Autowired
    private JooqDocumentQueryRepository systemUnderTest;

    @Autowired
    private DSLContext dslContext;

    @BeforeEach
    void setUp() {
        dslContext.deleteFrom(T_GENERATED_FILE).execute();
        dslContext.deleteFrom(T_GENERATED_DOCUMENT).execute();
    }

    @Test
    @DisplayName("Поиск по идентификатору возвращает агрегат документа с упорядоченными файлами")
    void givenDocumentWithFiles_whenFindById_thenReturnAggregate() {
        insertDocument(FIRST_DOCUMENT_ID, ENTITY_ID, FIRST_DOCUMENT_ID, REQUEST_ID_ONE, "2026-04-08T09:00:00+03:00");
        insertGeneratedFile(XLSX_FILE_ID, FIRST_DOCUMENT_ID, "XLSX", "2026-04-08T10:00:00+03:00");
        insertGeneratedFile(DOCX_FILE_ID, FIRST_DOCUMENT_ID, "DOCX", "2026-04-08T09:00:00+03:00");

        assertThat(systemUnderTest.findById(TENANT_ID, FIRST_DOCUMENT_ID))
            .hasValueSatisfying(document -> {
                assertThat(document.getId()).isEqualTo(FIRST_DOCUMENT_ID);
                assertThat(document.getTemplateId()).isEqualTo(TEMPLATE_ID);
                assertThat(document.getRequestId()).isEqualTo(REQUEST_ID_ONE);
                assertThat(document.getFiles())
                    .extracting(file -> file.getFormat(), file -> file.getStatus())
                    .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("DOCX", DocumentConstants.GeneratedFileStatus.PENDING),
                        org.assertj.core.groups.Tuple.tuple("XLSX", DocumentConstants.GeneratedFileStatus.PENDING)
                    );
            });
    }

    @Test
    @DisplayName("Поиск по идентификатору возвращает пустой результат, если документ не найден")
    void givenUnknownDocumentId_whenFindById_thenReturnEmptyOptional() {
        assertThat(systemUnderTest.findById(TENANT_ID, FIRST_DOCUMENT_ID)).isEmpty();
    }

    @Test
    @DisplayName("Поиск по объекту применяет сортировку и пагинацию и возвращает файлы документа")
    void givenMultipleDocuments_whenFindAllByObject_thenApplySortAndPaging() {
        insertDocument(FIRST_DOCUMENT_ID, ENTITY_ID, FIRST_DOCUMENT_ID, REQUEST_ID_ONE, "2026-04-08T09:00:00+03:00");
        insertDocument(SECOND_DOCUMENT_ID, ENTITY_ID, FIRST_DOCUMENT_ID, REQUEST_ID_TWO, "2026-04-08T11:00:00+03:00");
        insertDocument(THIRD_DOCUMENT_ID, ENTITY_ID, FIRST_DOCUMENT_ID, REQUEST_ID_THREE, "2026-04-08T13:00:00+03:00");
        insertDocument(
            UUID.fromString("39393939-3939-3939-3939-393939393939"),
            ENTITY_ID,
            OTHER_OBJECT_ID,
            UUID.fromString("40404040-4040-4040-4040-404040404040"),
            "2026-04-08T15:00:00+03:00"
        );

        insertGeneratedFile(DOCX_FILE_ID, FIRST_DOCUMENT_ID, "DOCX", "2026-04-08T09:00:00+03:00");
        insertGeneratedFile(XLSX_FILE_ID, SECOND_DOCUMENT_ID, "XLSX", "2026-04-08T11:00:00+03:00");
        insertGeneratedFile(PDF_FILE_ID, THIRD_DOCUMENT_ID, "PDF", "2026-04-08T13:00:00+03:00");

        CommonRqDto request = CommonRqDto.builder()
            .paging(PagingRqDto.builder().page(0).size(2).build())
            .sort(List.of(SortTypeDto.builder().field("createdAt").direction(SortTypeDto.Direction.DESC).build()))
            .build();

        List<Document> documents = systemUnderTest.findAllByObject(TENANT_ID, ENTITY_ID, FIRST_DOCUMENT_ID, request);

        assertThat(documents)
            .extracting(Document::getId)
            .containsExactly(THIRD_DOCUMENT_ID, SECOND_DOCUMENT_ID);
        assertThat(documents)
            .allSatisfy(document -> assertThat(document.getFiles()).hasSize(1));
    }

    @Test
    @DisplayName("Подсчет по объекту учитывает фильтры")
    void givenFilterRequest_whenCountByObject_thenCountOnlyMatchingDocuments() {
        insertDocument(FIRST_DOCUMENT_ID, ENTITY_ID, FIRST_DOCUMENT_ID, REQUEST_ID_ONE, "2026-04-08T09:00:00+03:00");
        insertDocument(SECOND_DOCUMENT_ID, ENTITY_ID, FIRST_DOCUMENT_ID, REQUEST_ID_TWO, "2026-04-08T11:00:00+03:00");
        insertDocument(
            UUID.fromString("41414141-4141-4141-4141-414141414141"),
            ENTITY_ID,
            OTHER_OBJECT_ID,
            REQUEST_ID_THREE,
            "2026-04-08T13:00:00+03:00"
        );

        CommonRqDto request = CommonRqDto.builder()
            .paging(PagingRqDto.builder().page(0).size(10).build())
            .filter(Set.of(
                FilterDto.builder()
                    .field("requestId")
                    .operation(FilterDto.Operation.EQUAL)
                    .value(List.of(REQUEST_ID_TWO.toString()))
                    .build()
            ))
            .build();

        assertThat(systemUnderTest.countByObject(TENANT_ID, ENTITY_ID, FIRST_DOCUMENT_ID, request)).isEqualTo(1);
    }

    private void insertDocument(
        UUID documentId,
        UUID entityId,
        UUID objectId,
        UUID requestId,
        String createdAt
    ) {
        dslContext.insertInto(T_GENERATED_DOCUMENT)
            .set(T_GENERATED_DOCUMENT.ID, documentId)
            .set(T_GENERATED_DOCUMENT.TENANT_ID, TENANT_ID)
            .set(T_GENERATED_DOCUMENT.TEMPLATE_ID, TEMPLATE_ID)
            .set(T_GENERATED_DOCUMENT.ENTITY_ID, entityId)
            .set(T_GENERATED_DOCUMENT.OBJECT_ID, objectId)
            .set(T_GENERATED_DOCUMENT.REQUEST_ID, requestId)
            .set(T_GENERATED_DOCUMENT.CREATED_AT, OffsetDateTime.parse(createdAt))
            .set(T_GENERATED_DOCUMENT.UPDATED_AT, OffsetDateTime.parse(createdAt))
            .set(T_GENERATED_DOCUMENT.CREATED_BY, USER_ID)
            .set(T_GENERATED_DOCUMENT.UPDATED_BY, USER_ID)
            .execute();
    }

    private void insertGeneratedFile(UUID fileId, UUID documentId, String format, String createdAt) {
        dslContext.insertInto(T_GENERATED_FILE)
            .set(T_GENERATED_FILE.ID, fileId)
            .set(T_GENERATED_FILE.TENANT_ID, TENANT_ID)
            .set(T_GENERATED_FILE.DOCUMENT_ID, documentId)
            .set(T_GENERATED_FILE.FORMAT, format)
            .set(T_GENERATED_FILE.STATUS, DocumentConstants.GeneratedFileStatus.PENDING)
            .set(T_GENERATED_FILE.CREATED_AT, OffsetDateTime.parse(createdAt))
            .set(T_GENERATED_FILE.UPDATED_AT, OffsetDateTime.parse(createdAt))
            .set(T_GENERATED_FILE.CREATED_BY, USER_ID)
            .set(T_GENERATED_FILE.UPDATED_BY, USER_ID)
            .execute();
    }
}
