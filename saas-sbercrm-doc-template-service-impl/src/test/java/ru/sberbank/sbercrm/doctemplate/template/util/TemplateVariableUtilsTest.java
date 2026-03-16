package ru.sberbank.sbercrm.doctemplate.template.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.sberbank.sbercrm.doctemplate.common.constant.CrmErrorCodes;
import ru.sberbank.sbercrm.doctemplate.common.exception.SystemCrmException;

class TemplateVariableUtilsTest {

    @Test
    @DisplayName("Компиляция шаблона переменной возвращает Pattern для корректного regex")
    void givenValidPlaceholderRegex_whenCompilePlaceholderPattern_thenReturnPattern() {
        // given
        String placeholderRegex = "\\$\\{([A-Za-z0-9_.$]+)}";

        // when
        Pattern pattern = TemplateVariableUtils.compilePlaceholderPattern(placeholderRegex);

        // then
        assertThat(pattern.pattern()).isEqualTo(placeholderRegex);
    }

    @Test
    @DisplayName("Компиляция шаблона переменной выбрасывает ошибку для regex без capture group")
    void givenRegexWithoutCaptureGroup_whenCompilePlaceholderPattern_thenThrowSystemException() {
        // given
        String placeholderRegex = "\\$\\{[A-Za-z0-9_.$]+}";

        // when // then
        assertThatThrownBy(() -> TemplateVariableUtils.compilePlaceholderPattern(placeholderRegex))
            .isInstanceOf(SystemCrmException.class)
            .satisfies(ex -> assertThat(((SystemCrmException) ex).getCode()).isEqualTo(CrmErrorCodes.TEMPLATE_VARIABLE_PATTERN_INVALID));
    }
}
