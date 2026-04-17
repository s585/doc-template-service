package ru.sberbank.sbercrm.saas.doctemplate.document.service.context;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.sberbank.sbercrm.saas.doctemplate.application.exception.model.BusinessCrmException;
import ru.sberbank.sbercrm.saas.doctemplate.application.integration.gateway.BusinessObjectGateway;
import ru.sberbank.sbercrm.saas.doctemplate.document.constant.DocumentConstants;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.GenerationJob;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.GenerationTemplateContext;
import ru.sberbank.sbercrm.saas.doctemplate.document.service.context.expression.NoOpExpressionEvaluator;
import ru.sberbank.sbercrm.saas.doctemplate.template.constant.TemplateConstants;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.Template;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.TemplateFormat;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.TemplateMapping;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.TemplateMappingDefinition;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.expression.PrimitiveExpression;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.source.ConstantValueSource;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.source.DirectValueSource;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.source.ReferenceValueSource;

@ExtendWith(MockitoExtension.class)
class GenerationContextAssemblerImplTest {
    private static final UUID TENANT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID ENTITY_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID OBJECT_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");

    @Mock
    private BusinessObjectGateway businessObjectGateway;

    private GenerationContextAssemblerImpl systemUnderTest;

    @BeforeEach
    void setUp() {
        systemUnderTest = new GenerationContextAssemblerImpl(
            List.of(new ConstantMappingValueResolver(), new DirectMappingValueResolver()),
            businessObjectGateway,
            new NoOpExpressionEvaluator()
        );
    }

    @Test
    @DisplayName("Assembler формирует values и generatedFileName для constant mappings")
    void givenConstantMappings_whenAssemble_thenBuildTemplateContext() {
        GenerationTemplateContext context = systemUnderTest.assemble(buildJob(), USER_ID, buildConstantTemplate());

        assertThat(context.getValues()).containsEntry("customer_name", "Romashka LLC");
        assertThat(context.getGeneratedFileName()).isEqualTo("contract.docx");
    }

    @Test
    @DisplayName("Assembler резолвит DIRECT mapping через business object gateway")
    void givenDirectMapping_whenAssemble_thenResolveValueFromBusinessObject() {
        given(businessObjectGateway.getObject(TENANT_ID, USER_ID, ENTITY_ID, OBJECT_ID))
            .willReturn(
                Map.of(
                    "customer", Map.of("name", "Direct LLC")
                )
            );

        GenerationTemplateContext context = systemUnderTest.assemble(buildJob(), USER_ID, buildDirectTemplate());

        assertThat(context.getValues()).containsEntry("customer_name", "Direct LLC");
        assertThat(context.getGeneratedFileName()).isEqualTo("contract.docx");
    }

    @Test
    @DisplayName("Skeleton evaluator не меняет значение при наличии expression")
    void givenExpression_whenAssemble_thenKeepResolvedSourceValue() {
        GenerationTemplateContext context = systemUnderTest.assemble(buildJob(), USER_ID, buildTemplateWithExpression());

        assertThat(context.getValues()).containsEntry("customer_name", "Romashka LLC");
    }

    @Test
    @DisplayName("Assembler выбрасывает business ошибку для неподдерживаемого source")
    void givenUnsupportedSource_whenAssemble_thenThrowBusinessException() {
        assertThatThrownBy(() -> systemUnderTest.assemble(buildJob(), USER_ID, buildUnsupportedSourceTemplate()))
            .isInstanceOf(BusinessCrmException.class)
            .hasMessage(DocumentConstants.ErrorCodes.GENERATION_MAPPING_SOURCE_UNSUPPORTED);
    }

    @Test
    @DisplayName("Assembler выбрасывает business ошибку для DIRECT path без field-сегментов")
    void givenDirectRootPath_whenAssemble_thenThrowBusinessException() {
        given(businessObjectGateway.getObject(TENANT_ID, USER_ID, ENTITY_ID, OBJECT_ID))
            .willReturn(Map.of("customer", Map.of("name", "Direct LLC")));

        assertThatThrownBy(() -> systemUnderTest.assemble(buildJob(), USER_ID, buildDirectRootTemplate()))
            .isInstanceOf(BusinessCrmException.class)
            .hasMessage(DocumentConstants.ErrorCodes.GENERATION_BUSINESS_OBJECT_PATH_INVALID);
    }

    private GenerationJob buildJob() {
        return GenerationJob.builder()
            .tenantId(TENANT_ID)
            .entityId(ENTITY_ID)
            .objectId(OBJECT_ID)
            .build();
    }

    private Template buildConstantTemplate() {
        return Template.builder()
            .name("fallback-name")
            .format(TemplateFormat.DOCX)
            .mappings(List.of(
                TemplateMapping.builder()
                    .key(TemplateConstants.MappingKeys.GENERATED_FILE_NAME)
                    .definition(
                        TemplateMappingDefinition.builder()
                            .source(ConstantValueSource.builder().value("contract").build())
                            .build()
                    )
                    .build(),
                TemplateMapping.builder()
                    .key("customer_name")
                    .definition(
                        TemplateMappingDefinition.builder()
                            .source(ConstantValueSource.builder().value("Romashka LLC").build())
                            .build()
                    )
                    .build()
            ))
            .build();
    }

    private Template buildDirectTemplate() {
        return Template.builder()
            .name("fallback-name")
            .format(TemplateFormat.DOCX)
            .mappings(List.of(
                TemplateMapping.builder()
                    .key(TemplateConstants.MappingKeys.GENERATED_FILE_NAME)
                    .definition(
                        TemplateMappingDefinition.builder()
                            .source(ConstantValueSource.builder().value("contract").build())
                            .build()
                    )
                    .build(),
                TemplateMapping.builder()
                    .key("customer_name")
                    .definition(
                        TemplateMappingDefinition.builder()
                            .source(DirectValueSource.builder().path("source.customer.name").build())
                            .build()
                    )
                    .build()
            ))
            .build();
    }

    private Template buildUnsupportedSourceTemplate() {
        return Template.builder()
            .name("fallback-name")
            .format(TemplateFormat.DOCX)
            .mappings(List.of(
                TemplateMapping.builder()
                    .key("customer_name")
                    .definition(
                        TemplateMappingDefinition.builder()
                            .source(
                                ReferenceValueSource.builder()
                                    .path("reference.customer.name")
                                    .build()
                            )
                            .build()
                    )
                    .build()
            ))
            .build();
    }

    private Template buildDirectRootTemplate() {
        return Template.builder()
            .name("fallback-name")
            .format(TemplateFormat.DOCX)
            .mappings(List.of(
                TemplateMapping.builder()
                    .key("customer_name")
                    .definition(
                        TemplateMappingDefinition.builder()
                            .source(DirectValueSource.builder().path("source").build())
                            .build()
                    )
                    .build()
            ))
            .build();
    }

    private Template buildTemplateWithExpression() {
        return Template.builder()
            .name("fallback-name")
            .format(TemplateFormat.DOCX)
            .mappings(List.of(
                TemplateMapping.builder()
                    .key("customer_name")
                    .definition(
                        TemplateMappingDefinition.builder()
                            .source(ConstantValueSource.builder().value("Romashka LLC").build())
                            .expression(PrimitiveExpression.builder().value("unused").build())
                            .build()
                    )
                    .build()
            ))
            .build();
    }
}
