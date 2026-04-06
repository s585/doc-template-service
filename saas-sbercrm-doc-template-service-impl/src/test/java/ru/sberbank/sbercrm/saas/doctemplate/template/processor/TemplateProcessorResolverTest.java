package ru.sberbank.sbercrm.saas.doctemplate.template.processor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.sberbank.sbercrm.saas.doctemplate.application.exception.model.SystemCrmException;
import ru.sberbank.sbercrm.saas.doctemplate.template.constant.TemplateConstants;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.TemplateFormat;

@ExtendWith(MockitoExtension.class)
class TemplateProcessorResolverTest {
    @Mock
    private FormatAwareTemplateProcessor docxProcessor;

    @Mock
    private FormatAwareTemplateProcessor xlsxProcessor;

    @Mock
    private FormatAwareTemplateProcessor duplicateDocxProcessor;

    @Test
    @DisplayName("Резолвер возвращает процессор по формату шаблона")
    void givenConfiguredProcessors_whenResolve_thenReturnMatchingProcessor() {
        // given
        given(docxProcessor.supports(TemplateFormat.DOCX)).willReturn(true);
        given(docxProcessor.supports(TemplateFormat.XLSX)).willReturn(false);
        given(xlsxProcessor.supports(TemplateFormat.DOCX)).willReturn(false);
        given(xlsxProcessor.supports(TemplateFormat.XLSX)).willReturn(true);

        // when
        TemplateProcessorResolver systemUnderTest =
            new TemplateProcessorResolver(List.of(docxProcessor, xlsxProcessor));

        // then
        assertThat(systemUnderTest.resolve(TemplateFormat.DOCX)).isSameAs(docxProcessor);
        assertThat(systemUnderTest.resolve(TemplateFormat.XLSX)).isSameAs(xlsxProcessor);
    }

    @Test
    @DisplayName("Резолвер падает на старте, если процессор для формата не настроен")
    void givenMissingProcessor_whenCreateResolver_thenThrowSystemCrmException() {
        // given
        given(docxProcessor.supports(TemplateFormat.DOCX)).willReturn(true);
        given(docxProcessor.supports(TemplateFormat.XLSX)).willReturn(false);
        List<FormatAwareTemplateProcessor> processors = List.of(docxProcessor);

        // expected
        assertThatThrownBy(() -> new TemplateProcessorResolver(processors))
            .isInstanceOf(SystemCrmException.class)
            .satisfies(exception ->
                assertThat(((SystemCrmException) exception).getCode())
                    .isEqualTo(TemplateConstants.ErrorCodes.TEMPLATE_PROCESSOR_MISSING)
            );
    }

    @Test
    @DisplayName("Резолвер падает на старте, если для формата настроено несколько процессоров")
    void givenDuplicateProcessors_whenCreateResolver_thenThrowSystemCrmException() {
        // given
        given(docxProcessor.supports(TemplateFormat.DOCX)).willReturn(true);
        given(duplicateDocxProcessor.supports(TemplateFormat.DOCX)).willReturn(true);
        List<FormatAwareTemplateProcessor> processors = List.of(docxProcessor, duplicateDocxProcessor, xlsxProcessor);

        // expected
        assertThatThrownBy(() -> new TemplateProcessorResolver(processors))
            .isInstanceOf(SystemCrmException.class)
            .satisfies(exception ->
                assertThat(((SystemCrmException) exception).getCode())
                    .isEqualTo(TemplateConstants.ErrorCodes.TEMPLATE_PROCESSOR_DUPLICATE)
            );
    }
}
