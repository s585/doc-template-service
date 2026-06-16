package ru.sberbank.sbercrm.saas.doctemplate.template.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.sberbank.sbercrm.saas.doctemplate.application.exception.model.BusinessCrmException;
import ru.sberbank.sbercrm.saas.doctemplate.template.constant.TemplateConstants;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.MappingScope;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.Template;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.TemplateMapping;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.TemplateMappingDefinition;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.TemplateValueType;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.source.DirectValueSource;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.source.ReferenceValueSource;
import ru.sberbank.sbercrm.saas.doctemplate.template.repository.TemplateMappingRepository;
import ru.sberbank.sbercrm.saas.doctemplate.template.repository.TemplateRepository;

@ExtendWith(MockitoExtension.class)
class TemplateServiceImplTest {
    private static final UUID TENANT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID TEMPLATE_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID USER_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID ENTITY_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");

    @Mock
    private TemplateRepository templateRepository;

    @Mock
    private TemplateMappingRepository templateMappingRepository;

    @Spy
    private TemplateMappingValidator templateMappingValidator;

    private TemplateServiceImpl systemUnderTest;

    @BeforeEach
    void setUp() {
        systemUnderTest = new TemplateServiceImpl(templateRepository, templateMappingRepository, templateMappingValidator);
    }

    @Test
    @DisplayName("Поиск по идентификатору использует агрегирующий метод и возвращает шаблон из репозитория")
    void whenFindById_thenDelegateToAggregateLookup() {
        // given
        Template template = Template.builder().id(TEMPLATE_ID).build();
        given(templateRepository.findById(TENANT_ID, TEMPLATE_ID)).willReturn(Optional.of(template));

        // when
        Optional<Template> result = systemUnderTest.findById(TENANT_ID, TEMPLATE_ID);

        // then
        assertThat(result).contains(template);
        verify(templateRepository).findById(TENANT_ID, TEMPLATE_ID);
    }

    @Test
    @DisplayName("Проверка существования делегирует дешевую проверку в репозиторий")
    void whenExists_thenDelegateToRepository() {
        // given
        given(templateRepository.exists(TENANT_ID, TEMPLATE_ID)).willReturn(true);

        // when
        boolean result = systemUnderTest.exists(TENANT_ID, TEMPLATE_ID);

        // then
        assertThat(result).isTrue();
        verify(templateRepository).exists(TENANT_ID, TEMPLATE_ID);
    }

    @Test
    @DisplayName("createMappings отклоняет generated_file_name вне FILE_NAME scope")
    void givenGeneratedFileNameWithInvalidScope_whenCreateMappings_thenThrowBusinessException() {
        List<TemplateMapping> mappings = List.of(invalidGeneratedFileNameMapping());

        assertThatThrownBy(() -> systemUnderTest.createMappings(TENANT_ID, TEMPLATE_ID, USER_ID, mappings))
            .isInstanceOf(BusinessCrmException.class)
            .satisfies(ex -> assertThat(((BusinessCrmException) ex).getCode())
                .isEqualTo(TemplateConstants.ErrorCodes.TEMPLATE_VARIABLE_INVALID));

        verify(templateMappingRepository, never()).createAll(TENANT_ID, TEMPLATE_ID, USER_ID, mappings);
    }

    @Test
    @DisplayName("createMappings отклоняет дублирующиеся ключи mapping-ов")
    void givenDuplicateMappingKeys_whenCreateMappings_thenThrowBusinessException() {
        List<TemplateMapping> mappings = List.of(
            validValueMapping("customer_name"),
            validValueMapping("customer_name")
        );

        assertThatThrownBy(() -> systemUnderTest.createMappings(TENANT_ID, TEMPLATE_ID, USER_ID, mappings))
            .isInstanceOf(BusinessCrmException.class)
            .satisfies(ex -> assertThat(((BusinessCrmException) ex).getCode())
                .isEqualTo(TemplateConstants.ErrorCodes.TEMPLATE_VARIABLE_INVALID));

        verify(templateMappingRepository, never()).createAll(TENANT_ID, TEMPLATE_ID, USER_ID, mappings);
    }

    @Test
    @DisplayName("createMappings отклоняет generated_file_name с REFERENCE source")
    void givenGeneratedFileNameWithReferenceSource_whenCreateMappings_thenThrowBusinessException() {
        List<TemplateMapping> mappings = List.of(invalidGeneratedFileNameReferenceMapping());

        assertThatThrownBy(() -> systemUnderTest.createMappings(TENANT_ID, TEMPLATE_ID, USER_ID, mappings))
            .isInstanceOf(BusinessCrmException.class)
            .satisfies(ex -> assertThat(((BusinessCrmException) ex).getCode())
                .isEqualTo(TemplateConstants.ErrorCodes.TEMPLATE_VARIABLE_INVALID));

        verify(templateMappingRepository, never()).createAll(TENANT_ID, TEMPLATE_ID, USER_ID, mappings);
    }

    @Test
    @DisplayName("createMappings отклоняет REFERENCE mapping вне COLLECTION scope")
    void givenReferenceMappingWithValueScope_whenCreateMappings_thenThrowBusinessException() {
        List<TemplateMapping> mappings = List.of(invalidReferenceMapping());

        assertThatThrownBy(() -> systemUnderTest.createMappings(TENANT_ID, TEMPLATE_ID, USER_ID, mappings))
            .isInstanceOf(BusinessCrmException.class)
            .satisfies(ex -> assertThat(((BusinessCrmException) ex).getCode())
                .isEqualTo(TemplateConstants.ErrorCodes.TEMPLATE_VARIABLE_INVALID));

        verify(templateMappingRepository, never()).createAll(TENANT_ID, TEMPLATE_ID, USER_ID, mappings);
    }

    @Test
    @DisplayName("replaceMappings отклоняет REFERENCE mapping вне COLLECTION scope до удаления старых mappings")
    void givenReferenceMappingWithValueScope_whenReplaceMappings_thenThrowBusinessException() {
        List<TemplateMapping> mappings = List.of(invalidReferenceMapping());

        assertThatThrownBy(() -> systemUnderTest.replaceMappings(TENANT_ID, TEMPLATE_ID, USER_ID, mappings))
            .isInstanceOf(BusinessCrmException.class)
            .satisfies(ex -> assertThat(((BusinessCrmException) ex).getCode())
                .isEqualTo(TemplateConstants.ErrorCodes.TEMPLATE_VARIABLE_INVALID));

        verify(templateMappingRepository, never()).deleteByTemplateId(TENANT_ID, TEMPLATE_ID);
        verify(templateMappingRepository, never()).createAll(TENANT_ID, TEMPLATE_ID, USER_ID, mappings);
    }

    @Test
    @DisplayName("createMappings отклоняет COLLECTION mapping без REFERENCE source")
    void givenCollectionMappingWithDirectSource_whenCreateMappings_thenThrowBusinessException() {
        List<TemplateMapping> mappings = List.of(invalidDirectCollectionMapping());

        assertThatThrownBy(() -> systemUnderTest.createMappings(TENANT_ID, TEMPLATE_ID, USER_ID, mappings))
            .isInstanceOf(BusinessCrmException.class)
            .satisfies(ex -> assertThat(((BusinessCrmException) ex).getCode())
                .isEqualTo(TemplateConstants.ErrorCodes.TEMPLATE_VARIABLE_INVALID));

        verify(templateMappingRepository, never()).createAll(TENANT_ID, TEMPLATE_ID, USER_ID, mappings);
    }

    private TemplateMapping invalidReferenceMapping() {
        return TemplateMapping.builder()
            .key("payment_id")
            .definition(
                TemplateMappingDefinition.builder()
                    .scope(MappingScope.VALUE)
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
                    .build()
            )
            .build();
    }

    private TemplateMapping validValueMapping(String key) {
        return TemplateMapping.builder()
            .key(key)
            .definition(
                TemplateMappingDefinition.builder()
                    .scope(MappingScope.VALUE)
                    .type(TemplateValueType.STRING)
                    .build()
            )
            .build();
    }

    private TemplateMapping invalidDirectCollectionMapping() {
        return TemplateMapping.builder()
            .key("contract_number_in_row")
            .definition(
                TemplateMappingDefinition.builder()
                    .scope(MappingScope.COLLECTION)
                    .type(TemplateValueType.STRING)
                    .source(DirectValueSource.builder().path("source.number").build())
                    .build()
            )
            .build();
    }

    private TemplateMapping invalidGeneratedFileNameMapping() {
        return TemplateMapping.builder()
            .key(TemplateConstants.MappingKeys.GENERATED_FILE_NAME)
            .definition(
                TemplateMappingDefinition.builder()
                    .scope(MappingScope.VALUE)
                    .type(TemplateValueType.STRING)
                    .source(DirectValueSource.builder().path("source.number").build())
                    .build()
            )
            .build();
    }

    private TemplateMapping invalidGeneratedFileNameReferenceMapping() {
        return TemplateMapping.builder()
            .key(TemplateConstants.MappingKeys.GENERATED_FILE_NAME)
            .definition(
                TemplateMappingDefinition.builder()
                    .scope(MappingScope.FILE_NAME)
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
                    .build()
            )
            .build();
    }
}
