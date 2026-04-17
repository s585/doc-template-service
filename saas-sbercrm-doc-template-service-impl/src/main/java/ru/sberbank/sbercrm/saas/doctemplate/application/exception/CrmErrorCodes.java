package ru.sberbank.sbercrm.saas.doctemplate.application.exception;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CrmErrorCodes {
    public static final String REQUEST_HEADER_MISSING = "request.header_missing";
    public static final String REQUEST_HEADER_INVALID = "request.header_invalid";
    public static final String FILE_STORAGE_REQUEST_FAILED = "file_storage.request_failed";
    public static final String CORE_CLIENT_REQUEST_FAILED = "core_client.request_failed";
    public static final String JSONB_SERIALIZATION_FAILED = "jsonb.serialization_failed";
    public static final String JSONB_DESERIALIZATION_FAILED = "jsonb.deserialization_failed";
    public static final String SYSTEM_UNEXPECTED = "system.unexpected";
}
