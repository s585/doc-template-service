package ru.sberbank.sbercrm.doctemplate.template.constant;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jooq.Field;

import java.util.Map;

import static ru.sberbank.sbercrm.jooq.tables.TTemplate.T_TEMPLATE;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TemplateJooqFieldMappings {
    public static final Map<String, Field<?>> FIELDS = Map.ofEntries(
        Map.entry("id", T_TEMPLATE.ID),
        Map.entry("entityId", T_TEMPLATE.ENTITY_ID),
        Map.entry("name", T_TEMPLATE.NAME),
        Map.entry("code", T_TEMPLATE.CODE),
        Map.entry("description", T_TEMPLATE.DESCRIPTION),
        Map.entry("format", T_TEMPLATE.FORMAT),
        Map.entry("s3Key", T_TEMPLATE.S3_KEY),
        Map.entry("active", T_TEMPLATE.ACTIVE),
        Map.entry("createdAt", T_TEMPLATE.CREATED_AT),
        Map.entry("updatedAt", T_TEMPLATE.UPDATED_AT),
        Map.entry("createdBy", T_TEMPLATE.CREATED_BY),
        Map.entry("updatedBy", T_TEMPLATE.UPDATED_BY)
    );
}
