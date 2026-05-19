package ru.sberbank.sbercrm.saas.doctemplate.template.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.checkerframework.checker.nullness.qual.Nullable;
import ru.sberbank.sbercrm.saas.doctemplate.template.constant.TemplateConstants;
import ru.sberbank.sbercrm.saas.doctemplate.application.exception.model.SystemCrmException;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.TemplateVariableInfo;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.MappingScope;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TemplateVariableUtils {
    public static Pattern compilePlaceholderPattern(String placeholderRegex) {
        try {
            Pattern pattern = Pattern.compile(placeholderRegex);
            if (pattern.matcher("").groupCount() < 1) {
                throw new SystemCrmException(
                    TemplateConstants.ErrorCodes.TEMPLATE_VARIABLE_PATTERN_INVALID,
                    TemplateConstants.ErrorCodes.TEMPLATE_VARIABLE_PATTERN_INVALID
                );
            }
            return pattern;
        } catch (SystemCrmException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new SystemCrmException(
                TemplateConstants.ErrorCodes.TEMPLATE_VARIABLE_PATTERN_INVALID,
                TemplateConstants.ErrorCodes.TEMPLATE_VARIABLE_PATTERN_INVALID,
                ex
            );
        }
    }

    public static List<TemplateVariableInfo> extractVariables(@Nullable String text, Pattern pattern, MappingScope scope) {
        List<TemplateVariableInfo> occurrences = new ArrayList<>();
        Matcher matcher = pattern.matcher(text == null ? "" : text);
        while (matcher.find()) {
            occurrences.add(
                TemplateVariableInfo.builder()
                    .key(matcher.group(1))
                    .scope(scope)
                    .build()
            );
        }
        return occurrences;
    }

    public static String replacePlaceholders(
        @Nullable String sourceText,
        Map<String, String> values,
        Pattern placeholderPattern
    ) {
        Matcher matcher = placeholderPattern.matcher(sourceText == null ? "" : sourceText);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            String placeholderKey = matcher.group(1);
            String placeholderValue = values.get(placeholderKey);
            if (placeholderValue == null) {
                matcher.appendReplacement(result, Matcher.quoteReplacement(matcher.group(0)));
                continue;
            }
            matcher.appendReplacement(result, Matcher.quoteReplacement(placeholderValue));
        }
        matcher.appendTail(result);
        return result.toString();
    }
}
