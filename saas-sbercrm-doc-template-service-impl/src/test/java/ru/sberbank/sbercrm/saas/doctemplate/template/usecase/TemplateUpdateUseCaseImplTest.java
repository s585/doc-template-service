package ru.sberbank.sbercrm.saas.doctemplate.template.usecase;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.sberbank.sbercrm.saas.doctemplate.template.constant.TemplateConstants;
import ru.sberbank.sbercrm.saas.doctemplate.application.exception.model.NotFoundCrmException;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.MappingScope;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.Template;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.TemplateMapping;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.TemplateMappingDefinition;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.TemplateMappingLayout;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.TemplateUpdateCmd;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.TemplateValueType;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.source.ConstantValueSource;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.source.DirectValueSource;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.source.ValueSourceKind;
import ru.sberbank.sbercrm.saas.doctemplate.template.service.TemplateService;

@ExtendWith(MockitoExtension.class)
class TemplateUpdateUseCaseImplTest {
    private static final UUID TENANT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID TEMPLATE_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @Mock
    private TemplateService templateService;

    @InjectMocks
    private TemplateUpdateUseCaseImpl systemUnderTest;

    @Test
    @DisplayName("Обновление шаблона выбрасывает 404, если шаблон не найден")
    void givenMissingTemplate_whenExecute_thenThrowNotFoundException() {
        // given
        TemplateUpdateCmd request = TemplateUpdateCmd.builder()
            .name("Обновленный шаблон")
            .active(true)
            .build();
        given(templateService.findById(TENANT_ID, TEMPLATE_ID)).willReturn(Optional.empty());

        // expected
        assertThatThrownBy(() -> systemUnderTest.execute(TENANT_ID, USER_ID, TEMPLATE_ID, request))
            .isInstanceOf(NotFoundCrmException.class)
            .satisfies(ex ->
                org.assertj.core.api.Assertions.assertThat(((NotFoundCrmException) ex).getCode())
                    .isEqualTo(TemplateConstants.ErrorCodes.TEMPLATE_NOT_FOUND)
            );

        verify(templateService).findById(TENANT_ID, TEMPLATE_ID);
        verify(templateService, never()).update(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("Обновление mapping сохраняет read-only layout из текущего шаблона")
    void givenUpdatedMappingWithoutLayout_whenExecute_thenPreserveCurrentLayout() {
        // given
        TemplateMappingLayout layout = TemplateMappingLayout.builder()
            .allowedSourceKinds(List.of(ValueSourceKind.CONSTANT, ValueSourceKind.DIRECT, ValueSourceKind.REFERENCE))
            .build();
        TemplateMapping currentMapping = TemplateMapping.builder()
            .key("product_name")
            .definition(TemplateMappingDefinition.builder()
                .scope(MappingScope.COLLECTION)
                .type(TemplateValueType.STRING)
                .source(DirectValueSource.builder().path("source.product.name").build())
                .layout(layout)
                .build())
            .build();
        Template currentTemplate = Template.builder()
            .id(TEMPLATE_ID)
            .name("Шаблон")
            .active(true)
            .mappings(List.of(currentMapping))
            .build();
        TemplateMapping updatedMapping = TemplateMapping.builder()
            .key("product_name")
            .definition(TemplateMappingDefinition.builder()
                .scope(MappingScope.COLLECTION)
                .type(TemplateValueType.STRING)
                .source(ConstantValueSource.builder().value("Товар").build())
                .build())
            .build();
        TemplateUpdateCmd request = TemplateUpdateCmd.builder()
            .name("Обновленный шаблон")
            .active(true)
            .mappings(List.of(updatedMapping))
            .build();
        given(templateService.findById(TENANT_ID, TEMPLATE_ID)).willReturn(Optional.of(currentTemplate));
        given(templateService.update(eq(TENANT_ID), any())).willReturn(currentTemplate);
        given(templateService.getMappings(TENANT_ID, TEMPLATE_ID)).willReturn(List.of(updatedMapping));

        // when
        systemUnderTest.execute(TENANT_ID, USER_ID, TEMPLATE_ID, request);

        // then
        ArgumentCaptor<List<TemplateMapping>> mappingsCaptor = ArgumentCaptor.captor();
        verify(templateService).replaceMappings(eq(TENANT_ID), eq(TEMPLATE_ID), eq(USER_ID), mappingsCaptor.capture());
        assertThat(mappingsCaptor.getValue())
            .singleElement()
            .extracting(mapping -> mapping.getDefinition().getLayout())
            .isSameAs(layout);
    }
}
