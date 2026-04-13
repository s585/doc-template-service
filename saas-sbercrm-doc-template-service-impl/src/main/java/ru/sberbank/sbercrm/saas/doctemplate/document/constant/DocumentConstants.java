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
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class DocumentStatus {
        public static final String QUEUED = "QUEUED";
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class GeneratedFileStatus {
        public static final String PENDING = "PENDING";
        public static final String PROCESSING = "PROCESSING";
        public static final String DONE = "DONE";
        public static final String ERROR = "ERROR";
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class GenerationJobStatus {
        public static final String QUEUED = "QUEUED";
        public static final String PROCESSING = "PROCESSING";
        public static final String DONE = "DONE";
        public static final String ERROR = "ERROR";
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class GenerationJobAttemptStatus {
        public static final String PROCESSING = "PROCESSING";
        public static final String DONE = "DONE";
        public static final String ERROR = "ERROR";
        public static final String TIMEOUT = "TIMEOUT";
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
