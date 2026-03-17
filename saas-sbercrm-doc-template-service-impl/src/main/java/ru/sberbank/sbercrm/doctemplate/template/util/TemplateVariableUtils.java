package ru.sberbank.sbercrm.doctemplate.template.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import ru.sberbank.sbercrm.doctemplate.common.constant.CrmErrorCodes;
import ru.sberbank.sbercrm.doctemplate.common.exception.SystemCrmException;
import ru.sberbank.sbercrm.doctemplate.template.model.TemplateVariableInfo;
import ru.sberbank.sbercrm.doctemplate.template.model.MappingScope;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TemplateVariableUtils {
    public static Pattern compilePlaceholderPattern(String placeholderRegex) {
        try {
            Pattern pattern = Pattern.compile(placeholderRegex);
            if (pattern.matcher("").groupCount() < 1) {
                throw new SystemCrmException(CrmErrorCodes.TEMPLATE_VARIABLE_PATTERN_INVALID);
            }
            return pattern;
        } catch (SystemCrmException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new SystemCrmException(ex, CrmErrorCodes.TEMPLATE_VARIABLE_PATTERN_INVALID);
        }
    }

    public static List<TemplateVariableInfo> extractOccurrences(String text, Pattern pattern, MappingScope scope) {
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
}
