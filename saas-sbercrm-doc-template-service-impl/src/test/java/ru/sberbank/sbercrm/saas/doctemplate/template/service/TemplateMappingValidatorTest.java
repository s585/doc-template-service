package ru.sberbank.sbercrm.saas.doctemplate.template.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.sberbank.sbercrm.saas.doctemplate.application.exception.model.BusinessCrmException;
import ru.sberbank.sbercrm.saas.doctemplate.template.constant.TemplateConstants;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.MappingScope;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.TemplateMapping;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.TemplateMappingDefinition;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.TemplateValueType;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.source.DirectValueSource;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.source.ReferenceValueSource;

class TemplateMappingValidatorTest {
    private static final UUID ENTITY_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");

    private final TemplateMappingValidator systemUnderTest = new TemplateMappingValidator();

    @Test
    @DisplayName("Валидатор пропускает null ключи, null definition и корректные reference collection mappings")
    void givenValidEdgeCaseMappings_whenValidate_thenDoNotThrow() {
        List<TemplateMapping> mappings = List.of(
            TemplateMapping.builder().key(null).definition(valueDefinition()).build(),
            TemplateMapping.builder().key("without_definition").definition(null).build(),
            TemplateMapping.builder().key("reference_collection").definition(referenceCollectionDefinition()).build(),
            TemplateMapping.builder().key("empty_collection").definition(collectionDefinitionWithoutSource()).build(),
            TemplateMapping.builder()
                .key(TemplateConstants.MappingKeys.GENERATED_FILE_NAME)
                .definition(fileNameDefinition())
                .build()
        );

        assertThatCode(() -> systemUnderTest.validate(mappings)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Валидатор отклоняет дублирующиеся ключи")
    void givenDuplicateKeys_whenValidate_thenThrowBusinessException() {
        List<TemplateMapping> mappings = List.of(
            TemplateMapping.builder().key("client_name").definition(valueDefinition()).build(),
            TemplateMapping.builder().key("client_name").definition(valueDefinition()).build()
        );

        assertThatThrownBy(() -> systemUnderTest.validate(mappings))
            .isInstanceOf(BusinessCrmException.class)
            .hasMessage(TemplateConstants.ErrorCodes.TEMPLATE_VARIABLE_INVALID);
    }

    private TemplateMappingDefinition valueDefinition() {
        return TemplateMappingDefinition.builder()
            .scope(MappingScope.VALUE)
            .type(TemplateValueType.STRING)
            .source(DirectValueSource.builder().path("source.name").build())
            .build();
    }

    private TemplateMappingDefinition fileNameDefinition() {
        return TemplateMappingDefinition.builder()
            .scope(MappingScope.FILE_NAME)
            .type(TemplateValueType.STRING)
            .source(DirectValueSource.builder().path("source.number").build())
            .build();
    }

    private TemplateMappingDefinition referenceCollectionDefinition() {
        return TemplateMappingDefinition.builder()
            .scope(MappingScope.COLLECTION)
            .type(TemplateValueType.STRING)
            .source(
                ReferenceValueSource.builder()
                    .entityId(ENTITY_ID)
                    .referenceFieldName("document$c")
                    .referenceValuePath("source.document$c.id")
                    .targetPath("source.document$c.payment$c")
                    .path("reference.paymentId")
                    .build()
            )
            .build();
    }

    private TemplateMappingDefinition collectionDefinitionWithoutSource() {
        return TemplateMappingDefinition.builder()
            .scope(MappingScope.COLLECTION)
            .type(TemplateValueType.STRING)
            .source(null)
            .build();
    }
}
