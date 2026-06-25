package ru.sberbank.sbercrm.saas.doctemplate.document.service.context.expression.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAccessor;
import java.util.function.Function;
import java.util.regex.Pattern;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import ru.sberbank.sbercrm.saas.doctemplate.application.exception.model.BusinessCrmException;
import ru.sberbank.sbercrm.saas.doctemplate.document.constant.DocumentConstants;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TemplateExpressionDateParser {
    private static final Pattern ISO_LOCAL_DATE_PATTERN = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");
    private static final Pattern ISO_LOCAL_DATE_TIME_PATTERN = Pattern.compile("\\d{4}-\\d{2}-\\d{2}T.+");
    private static final Pattern ISO_OFFSET_DATE_TIME_PATTERN = Pattern.compile("\\d{4}-\\d{2}-\\d{2}T.+(?:Z|[+-]\\d{2}:?\\d{2})");

    public static TemporalAccessor parse(String value, String mappingKey) {
        String trimmedValue = value.trim();
        if (ISO_LOCAL_DATE_PATTERN.matcher(trimmedValue).matches()) {
            return parse(trimmedValue, mappingKey, LocalDate::parse);
        }
        if (ISO_OFFSET_DATE_TIME_PATTERN.matcher(trimmedValue).matches()) {
            return parse(trimmedValue, mappingKey, OffsetDateTime::parse);
        }
        if (ISO_LOCAL_DATE_TIME_PATTERN.matcher(trimmedValue).matches()) {
            return parse(trimmedValue, mappingKey, LocalDateTime::parse);
        }
        throw invalid(mappingKey, value);
    }

    private static TemporalAccessor parse(
        String value,
        String mappingKey,
        Function<String, ? extends TemporalAccessor> parser
    ) {
        try {
            return parser.apply(value);
        } catch (DateTimeParseException exception) {
            throw invalid(mappingKey, value);
        }
    }

    private static BusinessCrmException invalid(String mappingKey, String value) {
        return new BusinessCrmException(
            DocumentConstants.ErrorCodes.GENERATION_EXPRESSION_INVALID,
            DocumentConstants.ErrorCodes.GENERATION_EXPRESSION_INVALID,
            mappingKey,
            "formatDate value is not an ISO date/datetime: " + value
        );
    }
}
