package ru.sberbank.sbercrm.saas.doctemplate.template.repository;

import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;
import ru.sberbank.sbercrm.saas.doctemplate.application.jooq.JsonbHelper;
import ru.sberbank.sbercrm.saas.doctemplate.template.converter.TemplateMappingRecordConverter;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.TemplateMapping;

import java.util.List;
import org.jooq.Query;
import java.util.UUID;

import static ru.sberbank.sbercrm.jooq.tables.TTemplateMapping.T_TEMPLATE_MAPPING;

@Repository
@RequiredArgsConstructor
public class JooqTemplateMappingRepository implements TemplateMappingRepository {
    private final DSLContext dslContext;
    private final TemplateMappingRecordConverter templateMappingRecordConverter;
    private final JsonbHelper jsonbHelper;

    @Override
    public List<TemplateMapping> findByTemplateId(UUID tenantId, UUID templateId) {
        return dslContext.selectFrom(T_TEMPLATE_MAPPING)
            .where(
                T_TEMPLATE_MAPPING.TENANT_ID.eq(tenantId),
                T_TEMPLATE_MAPPING.TEMPLATE_ID.eq(templateId)
            )
            .fetch(templateMappingRecordConverter);
    }

    @Override
    public void createAll(UUID tenantId, UUID templateId, UUID userId, List<TemplateMapping> mappings) {
        if (mappings == null || mappings.isEmpty()) {
            return;
        }

        List<Query> queries = mappings.stream()
            .map(mapping -> dslContext.insertInto(T_TEMPLATE_MAPPING)
                .set(T_TEMPLATE_MAPPING.TENANT_ID, tenantId)
                .set(T_TEMPLATE_MAPPING.TEMPLATE_ID, templateId)
                .set(T_TEMPLATE_MAPPING.KEY, mapping.getKey())
                .set(T_TEMPLATE_MAPPING.DEFINITION, jsonbHelper.toJsonb(mapping.getDefinition()))
                .set(T_TEMPLATE_MAPPING.CREATED_BY, userId)
                .set(T_TEMPLATE_MAPPING.UPDATED_BY, userId))
            .map(Query.class::cast)
            .toList();

        dslContext.batch(queries).execute();
    }

    @Override
    public void deleteByTemplateId(UUID tenantId, UUID templateId) {
        dslContext.deleteFrom(T_TEMPLATE_MAPPING)
            .where(
                T_TEMPLATE_MAPPING.TENANT_ID.eq(tenantId),
                T_TEMPLATE_MAPPING.TEMPLATE_ID.eq(templateId)
            )
            .execute();
    }
}
