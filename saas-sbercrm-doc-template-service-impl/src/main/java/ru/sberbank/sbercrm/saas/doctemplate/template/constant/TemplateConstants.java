package ru.sberbank.sbercrm.saas.doctemplate.template.constant;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jooq.Field;

import java.util.Map;

import static ru.sberbank.sbercrm.saas.doctemplate.jooq.tables.TTemplate.T_TEMPLATE;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TemplateConstants {

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class ErrorCodes {
        public static final String TEMPLATE_CODE_EXISTS = "template.code_exists";
        public static final String TEMPLATE_FILE_INVALID = "template.file_invalid";
        public static final String TEMPLATE_FORMAT_UNSUPPORTED = "template.format_unsupported";
        public static final String TEMPLATE_NOT_FOUND = "template.not_found";
        public static final String TEMPLATE_PARSING_FAILED = "template.parsing_failed";
        public static final String TEMPLATE_PROCESSOR_DUPLICATE = "template.processor.duplicate";
        public static final String TEMPLATE_PROCESSOR_MISSING = "template.processor.missing";
        public static final String TEMPLATE_VARIABLE_INVALID = "template.variable.invalid";
        public static final String TEMPLATE_VARIABLE_PATTERN_INVALID = "template.variable.pattern_invalid";
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class MappingKeys {
        public static final String GENERATED_FILE_NAME = "generated_file_name";
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class JooqFieldMappings {
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
}
