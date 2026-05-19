package ru.sberbank.sbercrm.saas.doctemplate.document.service.context;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.sberbank.sbercrm.saas.doctemplate.application.exception.model.BusinessCrmException;
import ru.sberbank.sbercrm.saas.doctemplate.document.constant.DocumentConstants;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.MappingScope;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.TemplateMapping;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.TemplateMappingDefinition;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.TemplateValueType;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.source.DirectValueSource;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.source.ReferenceValueSource;

class GenerationSourceValueResolverTest {
    private GenerationSourceValueResolver systemUnderTest;
    private final GenerationPathResolver generationPathResolver = new GenerationPathResolver();

    @BeforeEach
    void setUp() {
        systemUnderTest = new GenerationSourceValueResolver(
            List.of(new ConstantMappingValueResolver(), new DirectMappingValueResolver(generationPathResolver))
        );
    }

    @Test
    @DisplayName("Resolver выбрасывает business ошибку для неподдерживаемого source")
    void givenUnsupportedSource_whenResolve_thenThrowBusinessException() {
        TemplateMapping mapping = TemplateMapping.builder()
            .key("customer_name")
            .definition(
                TemplateMappingDefinition.builder()
                    .scope(MappingScope.VALUE)
                    .type(TemplateValueType.STRING)
                    .source(
                        ReferenceValueSource.builder()
                            .path("reference.customer.name")
                            .build()
                    )
                    .build()
            )
            .build();

        assertThatThrownBy(() -> systemUnderTest.resolve(mapping, null, null, null))
            .isInstanceOf(BusinessCrmException.class)
            .hasMessage(DocumentConstants.ErrorCodes.GENERATION_MAPPING_SOURCE_UNSUPPORTED);
    }

    @Test
    @DisplayName("Resolver выбрасывает business ошибку для DIRECT path без field-сегментов")
    void givenDirectRootPath_whenResolve_thenThrowBusinessException() {
        TemplateMapping mapping = TemplateMapping.builder()
            .key("customer_name")
            .definition(
                TemplateMappingDefinition.builder()
                    .scope(MappingScope.VALUE)
                    .type(TemplateValueType.STRING)
                    .source(DirectValueSource.builder().path("source").build())
                    .build()
            )
            .build();
        Map<String, Object> sourceObject = Map.of("customer", Map.of("name", "Direct LLC"));

        assertThatThrownBy(() -> systemUnderTest.resolve(mapping, sourceObject, null, null))
            .isInstanceOf(BusinessCrmException.class)
            .hasMessage(DocumentConstants.ErrorCodes.GENERATION_BUSINESS_OBJECT_PATH_INVALID);
    }
}
