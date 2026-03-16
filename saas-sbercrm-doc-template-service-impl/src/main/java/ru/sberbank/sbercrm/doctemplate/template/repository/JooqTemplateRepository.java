package ru.sberbank.sbercrm.doctemplate.template.repository;

import lombok.RequiredArgsConstructor;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;
import ru.sberbank.sbercrm.doctemplate.common.CommonRqDto;
import ru.sberbank.sbercrm.doctemplate.common.helper.JsonbHelper;
import ru.sberbank.sbercrm.doctemplate.common.jooq.JooqQueryBuilder;
import ru.sberbank.sbercrm.doctemplate.template.converter.TemplateMappingRecordConverter;
import ru.sberbank.sbercrm.doctemplate.template.converter.TemplateRecordConverter;
import ru.sberbank.sbercrm.doctemplate.template.constant.TemplateJooqFieldMappings;
import ru.sberbank.sbercrm.doctemplate.template.model.Template;
import ru.sberbank.sbercrm.doctemplate.template.model.TemplateMapping;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static ru.sberbank.sbercrm.jooq.tables.TTemplate.T_TEMPLATE;
import static ru.sberbank.sbercrm.jooq.tables.TTemplateMapping.T_TEMPLATE_MAPPING;

@Repository
@RequiredArgsConstructor
public class JooqTemplateRepository implements TemplateRepository {
    private final DSLContext dslContext;
    private final JsonbHelper jsonbHelper;
    private final JooqQueryBuilder jooqQueryBuilder;
    private final TemplateRecordConverter templateRecordConverter;
    private final TemplateMappingRecordConverter templateMappingRecordConverter;

    @Override
    public Template create(UUID tenantId, Template template) {
        return templateRecordConverter.convert(
            dslContext.insertInto(T_TEMPLATE)
                .set(T_TEMPLATE.TENANT_ID, tenantId)
                .set(T_TEMPLATE.ENTITY_ID, template.getEntityId())
                .set(T_TEMPLATE.NAME, template.getName())
                .set(T_TEMPLATE.CODE, template.getCode())
                .set(T_TEMPLATE.DESCRIPTION, template.getDescription())
                .set(T_TEMPLATE.FORMAT, template.getFormat().value())
                .set(T_TEMPLATE.S3_KEY, template.getS3Key())
                .set(T_TEMPLATE.ACTIVE, template.isActive())
                .set(T_TEMPLATE.DISPLAY_CONDITION, jsonbHelper.toJsonb(template.getDisplayCondition()))
                .set(T_TEMPLATE.CREATED_BY, template.getCreatedBy())
                .set(T_TEMPLATE.UPDATED_BY, template.getUpdatedBy())
                .returning()
                .fetchOne()
        );
    }

    @Override
    public Template update(UUID tenantId, Template template) {
        return templateRecordConverter.convert(
            dslContext.update(T_TEMPLATE)
                .set(T_TEMPLATE.ENTITY_ID, template.getEntityId())
                .set(T_TEMPLATE.NAME, template.getName())
                .set(T_TEMPLATE.CODE, template.getCode())
                .set(T_TEMPLATE.DESCRIPTION, template.getDescription())
                .set(T_TEMPLATE.FORMAT, template.getFormat().value())
                .set(T_TEMPLATE.S3_KEY, template.getS3Key())
                .set(T_TEMPLATE.ACTIVE, template.isActive())
                .set(T_TEMPLATE.DISPLAY_CONDITION, jsonbHelper.toJsonb(template.getDisplayCondition()))
                .set(T_TEMPLATE.UPDATED_BY, template.getUpdatedBy())
                .set(T_TEMPLATE.UPDATED_AT, DSL.currentOffsetDateTime())
                .where(
                    T_TEMPLATE.TENANT_ID.eq(tenantId),
                    T_TEMPLATE.ID.eq(template.getId())
                )
                .returning()
                .fetchOne()
        );
    }

    @Override
    public Optional<Template> findById(UUID tenantId, UUID templateId) {
        Field<List<TemplateMapping>> mappingsField = DSL.multiset(
            DSL.select(T_TEMPLATE_MAPPING.fields())
                .from(T_TEMPLATE_MAPPING)
                .where(
                    T_TEMPLATE_MAPPING.TENANT_ID.eq(T_TEMPLATE.TENANT_ID),
                    T_TEMPLATE_MAPPING.TEMPLATE_ID.eq(T_TEMPLATE.ID)
                )
        ).convertFrom(result -> result.map(templateMappingRecordConverter::convert));

        return dslContext.select(
                T_TEMPLATE.fields()
            )
            .select(mappingsField)
            .from(T_TEMPLATE)
            .where(
                T_TEMPLATE.TENANT_ID.eq(tenantId),
                T_TEMPLATE.ID.eq(templateId)
            )
            .fetchOptional()
            .map(record -> {
                Template template = templateRecordConverter.convert(record);
                template.setMappings(record.get(mappingsField));
                return template;
            });
    }

    @Override
    public List<Template> findAll(UUID tenantId, CommonRqDto request) {
        Condition condition = T_TEMPLATE.TENANT_ID.eq(tenantId)
            .and(jooqQueryBuilder.buildCondition(request.getFilter(), TemplateJooqFieldMappings.FIELDS));

        return dslContext.selectFrom(T_TEMPLATE)
            .where(condition)
            .orderBy(jooqQueryBuilder.buildOrderBy(request.getSort(), TemplateJooqFieldMappings.FIELDS))
            .limit(jooqQueryBuilder.buildLimit(request))
            .offset(jooqQueryBuilder.buildOffset(request))
            .fetch(templateRecordConverter::convert);
    }

    @Override
    public List<Template> findAllByEntityId(UUID tenantId, UUID entityId) {
        return dslContext.selectFrom(T_TEMPLATE)
            .where(
                T_TEMPLATE.TENANT_ID.eq(tenantId),
                T_TEMPLATE.ENTITY_ID.eq(entityId)
            )
            .fetch(templateRecordConverter::convert);
    }

    @Override
    public boolean existsByCode(UUID tenantId, String code, UUID excludedTemplateId) {
        Condition condition = DSL.and(
            T_TEMPLATE.TENANT_ID.eq(tenantId),
            T_TEMPLATE.CODE.eq(code)
        );

        if (excludedTemplateId != null) {
            condition = condition.and(T_TEMPLATE.ID.ne(excludedTemplateId));
        }

        return dslContext.fetchExists(T_TEMPLATE, condition);
    }

    @Override
    public void deleteById(UUID tenantId, UUID templateId) {
        dslContext.deleteFrom(T_TEMPLATE)
            .where(
                T_TEMPLATE.TENANT_ID.eq(tenantId),
                T_TEMPLATE.ID.eq(templateId)
            )
            .execute();
    }
}
