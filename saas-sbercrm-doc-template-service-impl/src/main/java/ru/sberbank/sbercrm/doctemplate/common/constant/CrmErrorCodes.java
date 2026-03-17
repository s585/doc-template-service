package ru.sberbank.sbercrm.doctemplate.common.constant;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CrmErrorCodes {
    public static final String REQUEST_HEADER_MISSING = "request.header_missing";
    public static final String REQUEST_HEADER_INVALID = "request.header_invalid";

    public static final String TEMPLATE_CODE_EXISTS = "template.code_exists";
    public static final String TEMPLATE_FILE_INVALID = "template.file_invalid";
    public static final String TEMPLATE_FORMAT_UNSUPPORTED = "template.format_unsupported";
    public static final String TEMPLATE_NOT_FOUND = "template.not_found";
    public static final String TEMPLATE_PARSING_FAILED = "template.parsing_failed";
    public static final String TEMPLATE_PROCESSOR_MISSING = "template.processor.missing";

    public static final String TEMPLATE_VARIABLE_INVALID = "template.variable.invalid";
    public static final String TEMPLATE_VARIABLE_PATTERN_INVALID = "template.variable.pattern_invalid";
}
