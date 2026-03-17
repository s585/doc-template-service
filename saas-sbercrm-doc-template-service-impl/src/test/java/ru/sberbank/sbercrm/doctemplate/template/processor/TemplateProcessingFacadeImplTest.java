package ru.sberbank.sbercrm.doctemplate.template.processor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.sberbank.sbercrm.doctemplate.template.model.MappingScope;
import ru.sberbank.sbercrm.doctemplate.template.model.TemplateFormat;
import ru.sberbank.sbercrm.doctemplate.template.model.TemplateVariableInfo;

@ExtendWith(MockitoExtension.class)
class TemplateProcessingFacadeImplTest {

    @Mock
    private TemplateProcessorResolver templateProcessorResolver;

    @Mock
    private FormatAwareTemplateProcessor templateProcessor;

    @InjectMocks
    private TemplateProcessingFacadeImpl templateProcessingFacade;

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
        List<TemplateVariableInfo> actualVariables = templateProcessingFacade.extractVariables(format, content);

        // then
        assertThat(actualVariables).isEqualTo(expectedVariables);
        verify(templateProcessorResolver).resolve(format);
        verify(templateProcessor).extractVariables(content);
    }
}
