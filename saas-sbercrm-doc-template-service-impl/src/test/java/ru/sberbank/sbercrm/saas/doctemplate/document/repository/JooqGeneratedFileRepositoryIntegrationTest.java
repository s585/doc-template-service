package ru.sberbank.sbercrm.saas.doctemplate.document.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.sberbank.sbercrm.saas.doctemplate.jooq.tables.TGeneratedDocument.T_GENERATED_DOCUMENT;
import static ru.sberbank.sbercrm.saas.doctemplate.jooq.tables.TGeneratedFile.T_GENERATED_FILE;

import java.util.List;
import java.util.UUID;
import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import ru.sberbank.sbercrm.saas.doctemplate.AbstractIntegrationTest;
import ru.sberbank.sbercrm.saas.doctemplate.document.constant.DocumentConstants;

@TestPropertySource(properties = "saas.doc-template.generation.enabled=false")
class JooqGeneratedFileRepositoryIntegrationTest extends AbstractIntegrationTest {
    private static final UUID DOCUMENT_ID = UUID.fromString("21212121-2121-2121-2121-212121212121");
    private static final UUID SECOND_DOCUMENT_ID = UUID.fromString("22222222-3333-4444-5555-666666666666");

    @Autowired
    private JooqGeneratedFileRepository systemUnderTest;

    @Autowired
    private DSLContext dslContext;

    @BeforeEach
    void setUp() {
        dslContext.deleteFrom(T_GENERATED_FILE).execute();
        dslContext.deleteFrom(T_GENERATED_DOCUMENT).execute();
    }

    @Test
    @DisplayName("Создание и методы чтения возвращают сгенерированные файлы документа")
    void givenCreatedFiles_whenFindMethodsCalled_thenReturnOrderedFiles() {
        insertDocument(DOCUMENT_ID, UUID.fromString("23232323-2323-2323-2323-232323232323"));
        insertDocument(SECOND_DOCUMENT_ID, UUID.fromString("24242424-2424-2424-2424-242424242424"));

        systemUnderTest.createAll(TENANT_ID, USER_ID, DOCUMENT_ID, List.of("DOCX", "XLSX"));
        systemUnderTest.createAll(TENANT_ID, USER_ID, SECOND_DOCUMENT_ID, List.of("PDF"));

        assertThat(systemUnderTest.findByDocumentId(TENANT_ID, DOCUMENT_ID))
            .extracting(file -> file.getFormat(), file -> file.getStatus())
            .containsExactlyInAnyOrder(
                org.assertj.core.groups.Tuple.tuple("DOCX", DocumentConstants.GeneratedFileStatus.PENDING),
                org.assertj.core.groups.Tuple.tuple("XLSX", DocumentConstants.GeneratedFileStatus.PENDING)
            );

        assertThat(systemUnderTest.findByDocumentIds(TENANT_ID, List.of(DOCUMENT_ID, SECOND_DOCUMENT_ID)))
            .extracting(file -> file.getDocumentId(), file -> file.getFormat())
            .containsExactlyInAnyOrder(
                org.assertj.core.groups.Tuple.tuple(DOCUMENT_ID, "DOCX"),
                org.assertj.core.groups.Tuple.tuple(DOCUMENT_ID, "XLSX"),
                org.assertj.core.groups.Tuple.tuple(SECOND_DOCUMENT_ID, "PDF")
            );
    }

    @Test
    @DisplayName("Поиск по идентификаторам документов возвращает пустой список при пустом наборе")
    void givenEmptyDocumentIds_whenFindByDocumentIds_thenReturnEmptyList() {
        assertThat(systemUnderTest.findByDocumentIds(TENANT_ID, List.of())).isEmpty();
        assertThat(systemUnderTest.findByDocumentIds(TENANT_ID, null)).isEmpty();
    }

    @Test
    @DisplayName("Перевод файла в обработку очищает предыдущую ошибку")
    void givenErroredFile_whenMarkProcessing_thenPersistProcessingState() {
        insertDocument(DOCUMENT_ID, UUID.fromString("25252525-2525-2525-2525-252525252525"));
        systemUnderTest.createAll(TENANT_ID, USER_ID, DOCUMENT_ID, List.of("DOCX"));
        systemUnderTest.markFailed(TENANT_ID, USER_ID, DOCUMENT_ID, "DOCX", "file.error", "File error");

        systemUnderTest.markProcessing(TENANT_ID, USER_ID, DOCUMENT_ID, "DOCX");

        assertThat(systemUnderTest.findByDocumentId(TENANT_ID, DOCUMENT_ID))
            .singleElement()
            .satisfies(file -> {
                assertThat(file.getStatus()).isEqualTo(DocumentConstants.GeneratedFileStatus.PROCESSING);
                assertThat(file.getErrorCode()).isNull();
                assertThat(file.getErrorMessage()).isNull();
                assertThat(file.getUpdatedBy()).isEqualTo(USER_ID);
            });
    }

    @Test
    @DisplayName("Завершение файла сохраняет метаданные и очищает предыдущую ошибку")
    void givenProcessingFile_whenMarkCompleted_thenPersistDoneState() {
        insertDocument(DOCUMENT_ID, UUID.fromString("26262626-2626-2626-2626-262626262626"));
        systemUnderTest.createAll(TENANT_ID, USER_ID, DOCUMENT_ID, List.of("DOCX"));
        systemUnderTest.markFailed(TENANT_ID, USER_ID, DOCUMENT_ID, "DOCX", "file.error", "File error");

        systemUnderTest.markCompleted(
            TENANT_ID,
            USER_ID,
            DOCUMENT_ID,
            "DOCX",
            "generated/file.docx",
            "checksum-value",
            128L
        );

        assertThat(systemUnderTest.findByDocumentId(TENANT_ID, DOCUMENT_ID))
            .singleElement()
            .satisfies(file -> {
                assertThat(file.getStatus()).isEqualTo(DocumentConstants.GeneratedFileStatus.DONE);
                assertThat(file.getS3Key()).isEqualTo("generated/file.docx");
                assertThat(file.getChecksum()).isEqualTo("checksum-value");
                assertThat(file.getSizeBytes()).isEqualTo(128L);
                assertThat(file.getErrorCode()).isNull();
                assertThat(file.getErrorMessage()).isNull();
                assertThat(file.getUpdatedBy()).isEqualTo(USER_ID);
            });
    }

    @Test
    @DisplayName("Фиксация ошибки сохраняет детали ошибки файла")
    void givenPendingFile_whenMarkFailed_thenPersistErrorState() {
        insertDocument(DOCUMENT_ID, UUID.fromString("27272727-2727-2727-2727-272727272727"));
        systemUnderTest.createAll(TENANT_ID, USER_ID, DOCUMENT_ID, List.of("DOCX"));

        systemUnderTest.markFailed(TENANT_ID, USER_ID, DOCUMENT_ID, "DOCX", "file.error", "File error");

        assertThat(systemUnderTest.findByDocumentId(TENANT_ID, DOCUMENT_ID))
            .singleElement()
            .satisfies(file -> {
                assertThat(file.getStatus()).isEqualTo(DocumentConstants.GeneratedFileStatus.ERROR);
                assertThat(file.getErrorCode()).isEqualTo("file.error");
                assertThat(file.getErrorMessage()).isEqualTo("File error");
                assertThat(file.getUpdatedBy()).isEqualTo(USER_ID);
            });
    }

    private void insertDocument(UUID documentId, UUID requestId) {
        dslContext.insertInto(T_GENERATED_DOCUMENT)
            .set(T_GENERATED_DOCUMENT.ID, documentId)
            .set(T_GENERATED_DOCUMENT.TENANT_ID, TENANT_ID)
            .set(T_GENERATED_DOCUMENT.TEMPLATE_ID, UUID.fromString("28282828-2828-2828-2828-282828282828"))
            .set(T_GENERATED_DOCUMENT.ENTITY_ID, ENTITY_ID)
            .set(T_GENERATED_DOCUMENT.OBJECT_ID, documentId)
            .set(T_GENERATED_DOCUMENT.REQUEST_ID, requestId)
            .set(T_GENERATED_DOCUMENT.CREATED_BY, USER_ID)
            .set(T_GENERATED_DOCUMENT.UPDATED_BY, USER_ID)
            .execute();
    }
}
