package ru.sberbank.sbercrm.saas.doctemplate.document.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;
import ru.sberbank.sbercrm.saas.doctemplate.application.jooq.JooqQueryBuilder;
import ru.sberbank.sbercrm.saas.doctemplate.document.constant.DocumentConstants;
import ru.sberbank.sbercrm.saas.doctemplate.document.converter.GeneratedDocumentRecordConverter;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.DocumentCreationCmd;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.GeneratedDocument;
import ru.sberbank.sbercrm.saas.doctemplate.shared.dto.CommonRqDto;

import static ru.sberbank.sbercrm.jooq.tables.TGeneratedDocument.T_GENERATED_DOCUMENT;

@Repository
@RequiredArgsConstructor
public class JooqGeneratedDocumentRepository implements GeneratedDocumentRepository {
    private final DSLContext dslContext;
    private final JooqQueryBuilder jooqQueryBuilder;
    private final GeneratedDocumentRecordConverter generatedDocumentRecordConverter;

    @Override
    public GeneratedDocument create(UUID tenantId, UUID userId, DocumentCreationCmd command) {
        return generatedDocumentRecordConverter.map(
            dslContext.insertInto(T_GENERATED_DOCUMENT)
                .set(T_GENERATED_DOCUMENT.TENANT_ID, tenantId)
                .set(T_GENERATED_DOCUMENT.TEMPLATE_ID, command.getTemplateId())
                .set(T_GENERATED_DOCUMENT.ENTITY_ID, command.getEntityId())
                .set(T_GENERATED_DOCUMENT.OBJECT_ID, command.getObjectId())
                .set(T_GENERATED_DOCUMENT.REQUEST_ID, command.getRequestId())
                .set(T_GENERATED_DOCUMENT.CREATED_BY, userId)
                .set(T_GENERATED_DOCUMENT.UPDATED_BY, userId)
                .returning()
                .fetchOne()
        );
    }

    @Override
    public Optional<GeneratedDocument> findById(UUID tenantId, UUID documentId) {
        return dslContext.selectFrom(T_GENERATED_DOCUMENT)
            .where(
                T_GENERATED_DOCUMENT.TENANT_ID.eq(tenantId),
                T_GENERATED_DOCUMENT.ID.eq(documentId)
            )
            .fetchOptional(generatedDocumentRecordConverter);
    }

    @Override
    public Optional<GeneratedDocument> findByRequestId(UUID tenantId, UUID requestId) {
        return dslContext.selectFrom(T_GENERATED_DOCUMENT)
            .where(
                T_GENERATED_DOCUMENT.TENANT_ID.eq(tenantId),
                T_GENERATED_DOCUMENT.REQUEST_ID.eq(requestId)
            )
            .orderBy(T_GENERATED_DOCUMENT.CREATED_AT.desc())
            .limit(1)
            .fetchOptional(generatedDocumentRecordConverter);
    }

    @Override
    public List<GeneratedDocument> findAllByObject(UUID tenantId, UUID entityId, UUID objectId, CommonRqDto request) {
        Condition condition = T_GENERATED_DOCUMENT.TENANT_ID.eq(tenantId)
            .and(T_GENERATED_DOCUMENT.ENTITY_ID.eq(entityId))
            .and(T_GENERATED_DOCUMENT.OBJECT_ID.eq(objectId))
            .and(jooqQueryBuilder.buildCondition(request.getFilter(), DocumentConstants.JooqFieldMappings.FIELDS));

        return dslContext.selectFrom(T_GENERATED_DOCUMENT)
            .where(condition)
            .orderBy(jooqQueryBuilder.buildOrderBy(request.getSort(), DocumentConstants.JooqFieldMappings.FIELDS))
            .limit(jooqQueryBuilder.buildLimit(request))
            .offset(jooqQueryBuilder.buildOffset(request))
            .fetch(generatedDocumentRecordConverter);
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
}
