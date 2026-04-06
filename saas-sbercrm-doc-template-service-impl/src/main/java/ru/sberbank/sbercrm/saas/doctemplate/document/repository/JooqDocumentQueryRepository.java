package ru.sberbank.sbercrm.saas.doctemplate.document.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;
import ru.sberbank.sbercrm.saas.doctemplate.application.jooq.JooqQueryBuilder;
import ru.sberbank.sbercrm.saas.doctemplate.document.constant.DocumentConstants;
import ru.sberbank.sbercrm.saas.doctemplate.document.converter.GeneratedDocumentRecordConverter;
import ru.sberbank.sbercrm.saas.doctemplate.document.converter.GeneratedFileRecordConverter;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.Document;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.GeneratedDocument;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.GeneratedFile;
import ru.sberbank.sbercrm.saas.doctemplate.shared.dto.CommonRqDto;

import static ru.sberbank.sbercrm.saas.doctemplate.jooq.tables.TGeneratedDocument.T_GENERATED_DOCUMENT;
import static ru.sberbank.sbercrm.saas.doctemplate.jooq.tables.TGeneratedFile.T_GENERATED_FILE;

@Repository
@RequiredArgsConstructor
public class JooqDocumentQueryRepository implements DocumentQueryRepository {
    private final DSLContext dslContext;
    private final JooqQueryBuilder jooqQueryBuilder;
    private final GeneratedDocumentRecordConverter generatedDocumentRecordConverter;
    private final GeneratedFileRecordConverter generatedFileRecordConverter;

    @Override
    public Optional<Document> findById(UUID tenantId, UUID documentId) {
        Field<List<GeneratedFile>> filesField = buildFilesField();

        return dslContext.select(T_GENERATED_DOCUMENT.fields())
            .select(filesField)
            .from(T_GENERATED_DOCUMENT)
            .where(
                T_GENERATED_DOCUMENT.TENANT_ID.eq(tenantId),
                T_GENERATED_DOCUMENT.ID.eq(documentId)
            )
            .fetchOptional(record -> toDocument(record, filesField));
    }

    @Override
    public List<Document> findAllByObject(UUID tenantId, UUID entityId, UUID objectId, CommonRqDto request) {
        Condition condition = T_GENERATED_DOCUMENT.TENANT_ID.eq(tenantId)
            .and(T_GENERATED_DOCUMENT.ENTITY_ID.eq(entityId))
            .and(T_GENERATED_DOCUMENT.OBJECT_ID.eq(objectId))
            .and(jooqQueryBuilder.buildCondition(request.getFilter(), DocumentConstants.JooqFieldMappings.FIELDS));
        Field<List<GeneratedFile>> filesField = buildFilesField();

        return dslContext.select(T_GENERATED_DOCUMENT.fields())
            .select(filesField)
            .from(T_GENERATED_DOCUMENT)
            .where(condition)
            .orderBy(jooqQueryBuilder.buildOrderBy(request.getSort(), DocumentConstants.JooqFieldMappings.FIELDS))
            .limit(jooqQueryBuilder.buildLimit(request))
            .offset(jooqQueryBuilder.buildOffset(request))
            .fetch(record -> toDocument(record, filesField));
    }

    @Override
    public long countByObject(UUID tenantId, UUID entityId, UUID objectId, CommonRqDto request) {
        Condition condition = T_GENERATED_DOCUMENT.TENANT_ID.eq(tenantId)
            .and(T_GENERATED_DOCUMENT.ENTITY_ID.eq(entityId))
            .and(T_GENERATED_DOCUMENT.OBJECT_ID.eq(objectId))
            .and(jooqQueryBuilder.buildCondition(request.getFilter(), DocumentConstants.JooqFieldMappings.FIELDS));

        return dslContext.fetchCount(
            dslContext.selectFrom(T_GENERATED_DOCUMENT).where(condition)
        );
    }

    private Field<List<GeneratedFile>> buildFilesField() {
        return DSL.multiset(
            DSL.select(T_GENERATED_FILE.fields())
                .from(T_GENERATED_FILE)
                .where(
                    T_GENERATED_FILE.TENANT_ID.eq(T_GENERATED_DOCUMENT.TENANT_ID),
                    T_GENERATED_FILE.DOCUMENT_ID.eq(T_GENERATED_DOCUMENT.ID)
                )
                .orderBy(T_GENERATED_FILE.CREATED_AT.asc(), T_GENERATED_FILE.ID.asc())
        ).convertFrom(result -> result.map(generatedFileRecordConverter));
    }

    private Document toDocument(Record record, Field<List<GeneratedFile>> filesField) {
        GeneratedDocument generatedDocument = generatedDocumentRecordConverter.map(record);

        return Document.builder()
            .id(generatedDocument.getId())
            .templateId(generatedDocument.getTemplateId())
            .entityId(generatedDocument.getEntityId())
            .objectId(generatedDocument.getObjectId())
            .requestId(generatedDocument.getRequestId())
            .files(record.get(filesField))
            .createdAt(generatedDocument.getCreatedAt())
            .createdBy(generatedDocument.getCreatedBy())
            .updatedAt(generatedDocument.getUpdatedAt())
            .updatedBy(generatedDocument.getUpdatedBy())
            .build();
    }
}
