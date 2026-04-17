package ru.sberbank.sbercrm.saas.doctemplate.document.constant;

import java.util.Map;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jooq.Field;

import static ru.sberbank.sbercrm.saas.doctemplate.jooq.tables.TGeneratedDocument.T_GENERATED_DOCUMENT;
import static ru.sberbank.sbercrm.saas.doctemplate.jooq.tables.TGenerationJob.T_GENERATION_JOB;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DocumentConstants {

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class ErrorCodes {
        public static final String DOCUMENT_NOT_FOUND = "document.not_found";
        public static final String GENERATION_JOB_NOT_FOUND = "generation.job_not_found";
        public static final String GENERATION_JOB_TRANSITION_INVALID = "generation.job_transition_invalid";
        public static final String GENERATION_JOB_TIMEOUT = "generation.job_timeout";
        public static final String GENERATION_MAPPING_SOURCE_UNSUPPORTED = "generation.mapping_source_unsupported";
        public static final String GENERATION_BUSINESS_OBJECT_NOT_FOUND = "generation.business_object_not_found";
        public static final String GENERATION_BUSINESS_OBJECT_PATH_INVALID = "generation.business_object_path_invalid";
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class JooqFieldMappings {
        public static final Map<String, Field<?>> FIELDS = Map.of(
            "id", T_GENERATED_DOCUMENT.ID,
            "templateId", T_GENERATED_DOCUMENT.TEMPLATE_ID,
            "requestId", T_GENERATED_DOCUMENT.REQUEST_ID,
            "createdAt", T_GENERATED_DOCUMENT.CREATED_AT,
            "updatedAt", T_GENERATED_DOCUMENT.UPDATED_AT
        );
    }
}
