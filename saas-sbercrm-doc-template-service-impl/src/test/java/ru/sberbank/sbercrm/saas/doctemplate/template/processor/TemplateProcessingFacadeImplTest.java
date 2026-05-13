package ru.sberbank.sbercrm.saas.doctemplate.template.processor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.GenerationTemplateContext;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.MappingScope;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.TemplateFormat;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.TemplateVariableInfo;

@ExtendWith(MockitoExtension.class)
class TemplateProcessingFacadeImplTest {

    @Mock
    private TemplateProcessorResolver templateProcessorResolver;

    @Mock
    private FormatAwareTemplateProcessor templateProcessor;

    @InjectMocks
    private TemplateProcessingFacadeImpl systemUnderTest;

    @Test
    @DisplayName("Фасад обработки шаблона делегирует извлечение переменных подходящему процессору")
    void givenFormatAndContent_whenExtractVariables_thenDelegateToResolvedProcessor() {
        // given
        TemplateFormat format = TemplateFormat.DOCX;
        byte[] content = {1, 2, 3};
        List<TemplateVariableInfo> expectedVariables = List.of(
            TemplateVariableInfo.builder()
                .key("deal_number")
                .scope(MappingScope.VALUE)
                .build()
        );
        given(templateProcessorResolver.resolve(format)).willReturn(templateProcessor);
        given(templateProcessor.extractVariables(content)).willReturn(expectedVariables);

        // when
        List<TemplateVariableInfo> actualVariables = systemUnderTest.extractVariables(format, content);

        // then
        assertThat(actualVariables).isEqualTo(expectedVariables);
        verify(templateProcessorResolver).resolve(format);
        verify(templateProcessor).extractVariables(content);
    }

    @Test
    @DisplayName("Фасад обработки шаблона делегирует генерацию подходящему процессору")
    void givenFormatContentAndValues_whenGenerate_thenDelegateToResolvedProcessor() {
        // given
        TemplateFormat format = TemplateFormat.DOCX;
        byte[] content = {1, 2, 3};
        GenerationTemplateContext context = GenerationTemplateContext.builder()
            .scalarValues(Map.of("deal_number", "123"))
            .build();
        byte[] expected = {4, 5, 6};
        given(templateProcessorResolver.resolve(format)).willReturn(templateProcessor);
        given(templateProcessor.generate(content, context)).willReturn(expected);

        // when
        byte[] actual = systemUnderTest.generate(format, content, context);

        // then
        assertThat(actual).isEqualTo(expected);
        verify(templateProcessorResolver).resolve(format);
        verify(templateProcessor).generate(content, context);
    }
}
