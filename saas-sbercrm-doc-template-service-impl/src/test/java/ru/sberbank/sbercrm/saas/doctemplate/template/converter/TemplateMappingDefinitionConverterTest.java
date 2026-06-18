package ru.sberbank.sbercrm.saas.doctemplate.template.converter;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import ru.sberbank.sbercrm.saas.doctemplate.template.constant.TemplateApiConstants;
import ru.sberbank.sbercrm.saas.doctemplate.template.dto.TemplateMappingDefinitionDto;
import ru.sberbank.sbercrm.saas.doctemplate.template.dto.TemplateMappingLayoutDto;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.MappingScope;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.TemplateMappingDefinition;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.TemplateMappingLayout;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.TemplateValueType;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.source.ValueSourceKind;

@SpringJUnitConfig(classes = {
    TemplateMappingDefinitionConverterImpl.class,
    ExpressionConverterImpl.class,
    ValueSourceConverterImpl.class,
    TemplateMappingLayoutConverterImpl.class
})
class TemplateMappingDefinitionConverterTest {
    @Autowired
    private TemplateMappingDefinitionConverter converter;

    @Test
    void shouldIgnoreLayoutWhenConvertingDtoToModel() {
        TemplateMappingDefinitionDto dto = TemplateMappingDefinitionDto.builder()
            .scope(MappingScope.VALUE.value())
            .type(TemplateValueType.STRING.value())
            .layout(TemplateMappingLayoutDto.builder()
                .allowedSourceKinds(List.of(TemplateApiConstants.ValueSourceJsonKinds.REFERENCE))
                .build())
            .build();

        TemplateMappingDefinition model = converter.convertToModel(dto);

        assertThat(model.getLayout()).isNull();
    }

    @Test
    void shouldExposeLayoutWhenConvertingModelToDto() {
        TemplateMappingDefinition model = TemplateMappingDefinition.builder()
            .scope(MappingScope.VALUE)
            .type(TemplateValueType.STRING)
            .layout(TemplateMappingLayout.builder()
                .allowedSourceKinds(List.of(
                    ValueSourceKind.CONSTANT,
                    ValueSourceKind.DIRECT
                ))
                .build())
            .build();

        TemplateMappingDefinitionDto dto = converter.convertToDto(model);

        assertThat(dto.getLayout().getAllowedSourceKinds())
            .containsExactly(
                TemplateApiConstants.ValueSourceJsonKinds.CONSTANT,
                TemplateApiConstants.ValueSourceJsonKinds.DIRECT
            );
    }
}
