package ru.sberbank.sbercrm.saas.doctemplate.document.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.sberbank.sbercrm.saas.doctemplate.jooq.tables.TGenerationJob.T_GENERATION_JOB;

import java.time.OffsetDateTime;
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
import ru.sberbank.sbercrm.saas.doctemplate.document.model.DocumentCreationCmd;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.GenerationJob;

@TestPropertySource(properties = "saas.doc-template.generation.enabled=false")
class JooqGenerationJobRepositoryIntegrationTest extends AbstractIntegrationTest {
    private static final UUID TEMPLATE_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID REQUEST_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID CLAIM_DOCUMENT_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final UUID WORKER_ID = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
    private static final UUID SECOND_WORKER_ID = UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee");
    private static final UUID ORDERED_DOCUMENT_ID = UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff");
    private static final UUID RECLAIM_DOCUMENT_ID = UUID.fromString("abababab-abab-abab-abab-abababababab");

    @Autowired
    private JooqGenerationJobRepository systemUnderTest;

    @Autowired
    private DSLContext dslContext;

    @BeforeEach
    void setUp() {
        dslContext.deleteFrom(T_GENERATION_JOB).execute();
    }

    @Test
    @DisplayName("Получение следующих задач соблюдает ограничение и переводит их в обработку с установкой лока")
    void givenQueuedJobs_whenClaimNextJobs_thenRespectLimitAndSetProcessingLock() {
        systemUnderTest.createAll(
            TENANT_ID,
            USER_ID,
            CLAIM_DOCUMENT_ID,
            DocumentCreationCmd.builder()
                .templateId(TEMPLATE_ID)
                .entityId(ENTITY_ID)
                .objectId(CLAIM_DOCUMENT_ID)
                .requestId(REQUEST_ID)
                .formats(List.of("DOCX", "XLSX", "PDF"))
                .build()
        );

        List<GenerationJob> claimedJobs = systemUnderTest.claimNextJobs(WORKER_ID, 2);

        assertThat(claimedJobs).hasSize(2);
        assertThat(claimedJobs)
            .extracting(GenerationJob::getStatus)
            .containsOnly(DocumentConstants.GenerationJobStatus.PROCESSING);

        List<UUID> claimedIds = claimedJobs.stream().map(GenerationJob::getId).toList();

        assertThat(
            dslContext.select(
                    T_GENERATION_JOB.STATUS,
                    T_GENERATION_JOB.LOCKED_BY,
                    T_GENERATION_JOB.LOCKED_UNTIL
                )
                .from(T_GENERATION_JOB)
                .where(T_GENERATION_JOB.ID.in(claimedIds))
                .fetch()
        )
            .allSatisfy(record -> {
                assertThat(record.get(T_GENERATION_JOB.STATUS))
                    .isEqualTo(DocumentConstants.GenerationJobStatus.PROCESSING);
                assertThat(record.get(T_GENERATION_JOB.LOCKED_BY)).isEqualTo(WORKER_ID);
                assertThat(record.get(T_GENERATION_JOB.LOCKED_UNTIL))
                    .isAfter(OffsetDateTime.now().minusMinutes(1));
            });
    }

    @Test
    @DisplayName("Получение следующих задач не забирает повторно уже залоченные задачи")
    void givenClaimedJobs_whenClaimNextJobsAgain_thenSkipAlreadyClaimedJobs() {
        systemUnderTest.createAll(
            TENANT_ID,
            USER_ID,
            RECLAIM_DOCUMENT_ID,
            DocumentCreationCmd.builder()
                .templateId(TEMPLATE_ID)
                .entityId(ENTITY_ID)
                .objectId(RECLAIM_DOCUMENT_ID)
                .requestId(REQUEST_ID)
                .formats(List.of("DOCX", "XLSX"))
                .build()
        );

        List<GenerationJob> firstClaim = systemUnderTest.claimNextJobs(WORKER_ID, 1);
        List<GenerationJob> secondClaim = systemUnderTest.claimNextJobs(SECOND_WORKER_ID, 5);

        assertThat(firstClaim).hasSize(1);
        assertThat(secondClaim).hasSize(1);
        assertThat(secondClaim.getFirst().getId()).isNotEqualTo(firstClaim.getFirst().getId());
        assertThat(systemUnderTest.claimNextJobs(SECOND_WORKER_ID, 5)).isEmpty();
    }

    @Test
    @DisplayName("Получение следующих задач возвращает их в порядке времени создания и идентификатора")
    void givenQueuedJobsWithKnownOrder_whenClaimNextJobs_thenReturnOrderedJobs() {
        systemUnderTest.createAll(
            TENANT_ID,
            USER_ID,
            ORDERED_DOCUMENT_ID,
            DocumentCreationCmd.builder()
                .templateId(TEMPLATE_ID)
                .entityId(ENTITY_ID)
                .objectId(ORDERED_DOCUMENT_ID)
                .requestId(REQUEST_ID)
                .formats(List.of("DOCX", "XLSX", "PDF"))
                .build()
        );

        UUID docxId = findJobIdByFormat(ORDERED_DOCUMENT_ID, "DOCX");
        UUID xlsxId = findJobIdByFormat(ORDERED_DOCUMENT_ID, "XLSX");
        UUID pdfId = findJobIdByFormat(ORDERED_DOCUMENT_ID, "PDF");

        dslContext.update(T_GENERATION_JOB)
            .set(T_GENERATION_JOB.CREATED_AT, OffsetDateTime.parse("2026-04-07T10:00:00+03:00"))
            .where(T_GENERATION_JOB.ID.eq(xlsxId))
            .execute();
        dslContext.update(T_GENERATION_JOB)
            .set(T_GENERATION_JOB.CREATED_AT, OffsetDateTime.parse("2026-04-07T09:00:00+03:00"))
            .where(T_GENERATION_JOB.ID.eq(pdfId))
            .execute();
        dslContext.update(T_GENERATION_JOB)
            .set(T_GENERATION_JOB.CREATED_AT, OffsetDateTime.parse("2026-04-07T11:00:00+03:00"))
            .where(T_GENERATION_JOB.ID.eq(docxId))
            .execute();

        List<GenerationJob> claimedJobs = systemUnderTest.claimNextJobs(WORKER_ID, 3);

        assertThat(claimedJobs)
            .extracting(GenerationJob::getId)
            .containsExactly(pdfId, xlsxId, docxId);
    }

    @Test
    @DisplayName("Получение следующих задач возвращает пустой список при неположительном ограничении")
    void givenNonPositiveLimit_whenClaimNextJobs_thenReturnEmptyList() {
        assertThat(systemUnderTest.claimNextJobs(WORKER_ID, 0)).isEmpty();
        assertThat(systemUnderTest.claimNextJobs(WORKER_ID, -1)).isEmpty();
    }

    @Test
    @DisplayName("Завершение задачи снимает лок, очищает ошибки и переводит её в завершенное состояние")
    void givenProcessingJob_whenMarkCompleted_thenPersistDoneState() {
        systemUnderTest.createAll(
            TENANT_ID,
            USER_ID,
            CLAIM_DOCUMENT_ID,
            buildCreationCommand(CLAIM_DOCUMENT_ID, REQUEST_ID, List.of("DOCX"))
        );

        GenerationJob claimedJob = systemUnderTest.claimNextJobs(WORKER_ID, 1).getFirst();
        dslContext.update(T_GENERATION_JOB)
            .set(T_GENERATION_JOB.ERROR_CODE, "some.error")
            .set(T_GENERATION_JOB.ERROR_MESSAGE, "some message")
            .where(T_GENERATION_JOB.ID.eq(claimedJob.getId()))
            .execute();

        assertThat(systemUnderTest.markCompleted(TENANT_ID, USER_ID, claimedJob.getId(), 0, 1)).isTrue();

        GenerationJob persistedJob = systemUnderTest.findById(TENANT_ID, claimedJob.getId()).orElseThrow();
        assertThat(persistedJob.getStatus()).isEqualTo(DocumentConstants.GenerationJobStatus.DONE);
        assertThat(persistedJob.getErrorCode()).isNull();
        assertThat(persistedJob.getErrorMessage()).isNull();
        assertThat(persistedJob.getUpdatedBy()).isEqualTo(USER_ID);
        assertThat(
            dslContext.select(T_GENERATION_JOB.LOCKED_BY, T_GENERATION_JOB.LOCKED_UNTIL)
                .from(T_GENERATION_JOB)
                .where(T_GENERATION_JOB.ID.eq(claimedJob.getId()))
                .fetchSingle()
        )
            .satisfies(record -> {
                assertThat(record.get(T_GENERATION_JOB.LOCKED_BY)).isNull();
                assertThat(record.get(T_GENERATION_JOB.LOCKED_UNTIL)).isNull();
            });
    }

    @Test
    @DisplayName("Фиксация ошибки снимает лок и сохраняет детали ошибки")
    void givenProcessingJob_whenMarkFailed_thenPersistErrorState() {
        systemUnderTest.createAll(
            TENANT_ID,
            USER_ID,
            CLAIM_DOCUMENT_ID,
            buildCreationCommand(CLAIM_DOCUMENT_ID, REQUEST_ID, List.of("DOCX"))
        );

        GenerationJob claimedJob = systemUnderTest.claimNextJobs(WORKER_ID, 1).getFirst();

        assertThat(
            systemUnderTest.markFailed(
                TENANT_ID,
                USER_ID,
                claimedJob.getId(),
                0,
                1,
                "generation.failed",
                "Generation failed"
            )
        ).isTrue();

        GenerationJob persistedJob = systemUnderTest.findById(TENANT_ID, claimedJob.getId()).orElseThrow();
        assertThat(persistedJob.getStatus()).isEqualTo(DocumentConstants.GenerationJobStatus.ERROR);
        assertThat(persistedJob.getErrorCode()).isEqualTo("generation.failed");
        assertThat(persistedJob.getErrorMessage()).isEqualTo("Generation failed");
        assertThat(persistedJob.getUpdatedBy()).isEqualTo(USER_ID);
        assertThat(
            dslContext.select(T_GENERATION_JOB.LOCKED_BY, T_GENERATION_JOB.LOCKED_UNTIL)
                .from(T_GENERATION_JOB)
                .where(T_GENERATION_JOB.ID.eq(claimedJob.getId()))
                .fetchSingle()
        )
            .satisfies(record -> {
                assertThat(record.get(T_GENERATION_JOB.LOCKED_BY)).isNull();
                assertThat(record.get(T_GENERATION_JOB.LOCKED_UNTIL)).isNull();
            });
    }

    private DocumentCreationCmd buildCreationCommand(UUID objectId, UUID requestId, List<String> formats) {
        return DocumentCreationCmd.builder()
            .templateId(TEMPLATE_ID)
            .entityId(ENTITY_ID)
            .objectId(objectId)
            .requestId(requestId)
            .formats(formats)
            .build();
    }

    private UUID findJobIdByFormat(UUID documentId, String format) {
        return dslContext.select(T_GENERATION_JOB.ID)
            .from(T_GENERATION_JOB)
            .where(
                T_GENERATION_JOB.TENANT_ID.eq(TENANT_ID),
                T_GENERATION_JOB.DOCUMENT_ID.eq(documentId),
                T_GENERATION_JOB.FORMAT.eq(format)
            )
            .fetchSingle(T_GENERATION_JOB.ID);
    }
}
