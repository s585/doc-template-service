package ru.sberbank.sbercrm.saas.doctemplate.document.service.context;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.sberbank.sbercrm.saas.doctemplate.application.integration.gateway.BusinessObjectGateway;
import ru.sberbank.sbercrm.saas.doctemplate.application.exception.model.BusinessCrmException;
import ru.sberbank.sbercrm.saas.doctemplate.application.pagination.PageResult;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.CollectionDataset;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.GenerationJob;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.GenerationTemplateContext;
import ru.sberbank.sbercrm.saas.doctemplate.document.service.context.expression.ExpressionEvaluator;
import ru.sberbank.sbercrm.saas.doctemplate.document.service.context.expression.NoOpExpressionEvaluator;
import ru.sberbank.sbercrm.saas.doctemplate.shared.dto.PagingRqDto;
import ru.sberbank.sbercrm.saas.doctemplate.shared.dto.PagingRsDto;
import ru.sberbank.sbercrm.saas.doctemplate.shared.dto.SelectDto;
import ru.sberbank.sbercrm.saas.doctemplate.template.constant.TemplateConstants;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.MappingScope;
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
        systemUnderTest = createAssembler(new NoOpExpressionEvaluator());
    }

    @Test
    @DisplayName("Assembler формирует values и generatedFileName для constant mappings")
    void givenConstantMappings_whenAssemble_thenBuildTemplateContext() {
        GenerationTemplateContext context = systemUnderTest.assemble(buildJob(), USER_ID, buildConstantTemplate());

        assertThat(context.getScalarValues()).containsEntry("customer_name", "Romashka LLC");
        assertThat(context.getGeneratedFileName()).isEqualTo("contract.docx");
    }

    @Test
    @DisplayName("Assembler резолвит DIRECT mapping через business object gateway")
    void givenDirectMapping_whenAssemble_thenResolveValueFromBusinessObject() {
        SelectDto expectedSelect = SelectDto.builder().fields(Set.of("customer.name")).build();
        given(businessObjectGateway.getObject(TENANT_ID, USER_ID, ENTITY_ID, OBJECT_ID, expectedSelect))
            .willReturn(
                Map.of(
                    "customer", Map.of("name", "Direct LLC")
                )
            );

        GenerationTemplateContext context = systemUnderTest.assemble(buildJob(), USER_ID, buildDirectTemplate());

        assertThat(context.getScalarValues()).containsEntry("customer_name", "Direct LLC");
        assertThat(context.getGeneratedFileName()).isEqualTo("contract.docx");
    }

    @Test
    @DisplayName("Skeleton evaluator не меняет значение при наличии expression")
    void givenExpression_whenAssemble_thenKeepResolvedSourceValue() {
        GenerationTemplateContext context = systemUnderTest.assemble(buildJob(), USER_ID, buildTemplateWithExpression());

        assertThat(context.getScalarValues()).containsEntry("customer_name", "Romashka LLC");
    }

    @Test
    @DisplayName("Assembler загружает reference collection один раз на группу и собирает все страницы")
    void givenReferenceCollectionMappings_whenAssemble_thenGroupLookupAndMergeAllPages() {
        SelectDto expectedSelect = SelectDto.builder().fields(Set.of("document$c.id", "document$c.dealProduct$c")).build();
        given(businessObjectGateway.getObject(TENANT_ID, USER_ID, ENTITY_ID, OBJECT_ID, expectedSelect))
            .willReturn(Map.of("document$c", Map.of("id", "doc-1", "dealProduct$c", List.of())));
        given(businessObjectGateway.getListObjectsPage(eq(TENANT_ID), eq(USER_ID), eq(ENTITY_ID), any()))
            .willReturn(
                PageResult.<Map<String, Object>>builder()
                    .data(List.of(Map.of("product", Map.of("name", "Product A"), "quantity", 2)))
                    .paging(PagingRsDto.builder()
                        .currentPage(0L)
                        .totalPageAmount(2L)
                        .recordsOnPage(100L)
                        .build())
                    .build(),
                PageResult.<Map<String, Object>>builder()
                    .data(List.of(Map.of("product", Map.of("name", "Product B"), "quantity", 1)))
                    .paging(PagingRsDto.builder()
                        .currentPage(1L)
                        .totalPageAmount(2L)
                        .recordsOnPage(1L)
                        .build())
                    .build()
            );

        GenerationTemplateContext context = systemUnderTest.assemble(buildJob(), USER_ID, buildReferenceCollectionTemplate());

        assertThat(context.getCollections()).hasSize(1);
        CollectionDataset dataset = context.getCollections().getFirst();
        assertThat(dataset.getKeys()).containsExactly("product_name", "product_qty");
        assertThat(dataset.getRows()).containsExactly(
            Map.of("product_name", "Product A", "product_qty", "2"),
            Map.of("product_name", "Product B", "product_qty", "1")
        );
        verify(businessObjectGateway, times(2)).getListObjectsPage(eq(TENANT_ID), eq(USER_ID), eq(ENTITY_ID), any());
    }

    @Test
    @DisplayName("Assembler применяет expression к каждому значению reference collection")
    void givenReferenceCollectionExpression_whenAssemble_thenEvaluateEachRowValue() {
        ExpressionEvaluator expressionEvaluator = mock(ExpressionEvaluator.class);
        given(expressionEvaluator.evaluate(any(), any())).willAnswer(invocation -> {
            Object value = invocation.getArgument(1);
            return value == null ? null : value + " transformed";
        });
        systemUnderTest = createAssembler(expressionEvaluator);

        SelectDto expectedSelect = SelectDto.builder().fields(Set.of("document$c.id", "document$c.dealProduct$c")).build();
        given(businessObjectGateway.getObject(TENANT_ID, USER_ID, ENTITY_ID, OBJECT_ID, expectedSelect))
            .willReturn(Map.of("document$c", Map.of("id", "doc-1", "dealProduct$c", List.of())));
        given(businessObjectGateway.getListObjectsPage(eq(TENANT_ID), eq(USER_ID), eq(ENTITY_ID), any()))
            .willReturn(
                PageResult.<Map<String, Object>>builder()
                    .data(List.of(Map.of("product", Map.of("name", "Product A"), "quantity", 2)))
                    .paging(PagingRsDto.builder()
                        .currentPage(0L)
                        .totalPageAmount(1L)
                        .recordsOnPage(1L)
                        .build())
                    .build()
            );

        GenerationTemplateContext context = systemUnderTest.assemble(buildJob(), USER_ID, buildReferenceCollectionTemplate());

        assertThat(context.getCollections()).singleElement().satisfies(dataset ->
            assertThat(dataset.getRows()).containsExactly(
                Map.of("product_name", "Product A transformed", "product_qty", "2 transformed")
            )
        );
    }

    @Test
    @DisplayName("Assembler падает до рендера, если часть collection mappings не попала ни в один dataset")
    void givenMissingCollectionDatasetKey_whenAssemble_thenThrowBusinessException() {
        SelectDto expectedSelect = SelectDto.builder().fields(Set.of("document$c.id", "document$c.dealProduct$c")).build();
        given(businessObjectGateway.getObject(TENANT_ID, USER_ID, ENTITY_ID, OBJECT_ID, expectedSelect))
            .willReturn(Map.of("document$c", Map.of("id", "doc-1", "dealProduct$c", List.of())));
        given(businessObjectGateway.getListObjectsPage(eq(TENANT_ID), eq(USER_ID), eq(ENTITY_ID), any()))
            .willReturn(
                PageResult.<Map<String, Object>>builder()
                    .data(List.of(Map.of("product", Map.of("name", "Product A"), "quantity", 2)))
                    .paging(PagingRsDto.builder()
                        .currentPage(0L)
                        .totalPageAmount(1L)
                        .recordsOnPage(1L)
                        .build())
                    .build()
            );

        assertThatThrownBy(() -> systemUnderTest.assemble(buildJob(), USER_ID, buildReferenceCollectionTemplateWithMissingKey()))
            .isInstanceOf(BusinessCrmException.class)
            .satisfies(throwable -> {
                BusinessCrmException exception = (BusinessCrmException) throwable;
                assertThat(exception.getCode()).isEqualTo(TemplateConstants.ErrorCodes.TEMPLATE_COLLECTION_PLACEHOLDERS_MISSING_DATASET);
                assertThat(exception.getParams()).containsExactly("[product_qty]");
            });
    }

    @Test
    @DisplayName("Assembler падает до рендера, если один collection key появляется в нескольких dataset'ах")
    void givenDuplicateCollectionKeyAcrossDatasets_whenAssemble_thenThrowBusinessException() {
        given(businessObjectGateway.getObject(eq(TENANT_ID), eq(USER_ID), eq(ENTITY_ID), eq(OBJECT_ID), any()))
            .willReturn(
                Map.of(
                    "document$c", Map.of("id", "doc-1", "dealProduct$c", List.of()),
                    "contract$c", Map.of("id", "contract-1", "dealProduct$c", List.of())
                )
            );
        given(businessObjectGateway.getListObjectsPage(eq(TENANT_ID), eq(USER_ID), eq(ENTITY_ID), any()))
            .willReturn(
                PageResult.<Map<String, Object>>builder()
                    .data(List.of(Map.of("product", Map.of("name", "Product A"), "quantity", 2)))
                    .paging(PagingRsDto.builder()
                        .currentPage(0L)
                        .totalPageAmount(2L)
                        .recordsOnPage(100L)
                        .build())
                    .build(),
                PageResult.<Map<String, Object>>builder()
                    .data(List.of(Map.of("product", Map.of("name", "Product B"), "quantity", 3)))
                    .paging(PagingRsDto.builder()
                        .currentPage(1L)
                        .totalPageAmount(2L)
                        .recordsOnPage(1L)
                        .build())
                    .build()
            );

        assertThatThrownBy(() -> systemUnderTest.assemble(buildJob(), USER_ID, buildDuplicateCollectionKeyTemplate()))
            .isInstanceOf(BusinessCrmException.class)
            .satisfies(throwable -> {
                BusinessCrmException exception = (BusinessCrmException) throwable;
                assertThat(exception.getCode()).isEqualTo(TemplateConstants.ErrorCodes.TEMPLATE_COLLECTION_PLACEHOLDERS_AMBIGUOUS);
                assertThat(exception.getParams()).containsExactly("[product_name]");
            });
    }

    private GenerationContextAssemblerImpl createAssembler(ExpressionEvaluator expressionEvaluator) {
        GenerationPathResolver generationPathResolver = new GenerationPathResolver();
        GenerationSourceValueResolver sourceValueResolver = new GenerationSourceValueResolver(
            List.of(new ConstantMappingValueResolver(), new DirectMappingValueResolver(generationPathResolver))
        );
        GenerationCollectionDatasetResolver collectionDatasetResolver = new GenerationCollectionDatasetResolver(
            List.of(new ReferenceCollectionDatasetResolver(
                businessObjectGateway,
                generationPathResolver,
                expressionEvaluator
            ))
        );
        GenerationMappingPlanner generationMappingPlanner = new GenerationMappingPlanner(collectionDatasetResolver);
        return new GenerationContextAssemblerImpl(
            businessObjectGateway,
            new GenerationSelectBuilder(generationPathResolver),
            generationMappingPlanner,
            sourceValueResolver,
            collectionDatasetResolver,
            expressionEvaluator
        );
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

    private Template buildReferenceCollectionTemplate() {
        ReferenceValueSource referenceSource = ReferenceValueSource.builder()
            .entityId(ENTITY_ID)
            .targetPath("source.document$c.dealProduct$c")
            .referenceFieldName("document$c")
            .referenceValuePath("source.document$c.id")
            .paging(PagingRqDto.builder().page(0).size(100).build())
            .build();
        return Template.builder()
            .name("fallback-name")
            .format(TemplateFormat.DOCX)
            .mappings(List.of(
                TemplateMapping.builder()
                    .key("product_name")
                    .definition(
                        TemplateMappingDefinition.builder()
                            .scope(MappingScope.COLLECTION)
                            .source(referenceSource.toBuilder().path("reference.product.name").build())
                            .build()
                    )
                    .build(),
                TemplateMapping.builder()
                    .key("product_qty")
                    .definition(
                        TemplateMappingDefinition.builder()
                            .scope(MappingScope.COLLECTION)
                            .source(referenceSource.toBuilder().path("reference.quantity").build())
                            .build()
                    )
                    .build()
            ))
            .build();
    }

    private Template buildReferenceCollectionTemplateWithMissingKey() {
        ReferenceValueSource referenceSource = ReferenceValueSource.builder()
            .entityId(ENTITY_ID)
            .targetPath("source.document$c.dealProduct$c")
            .referenceFieldName("document$c")
            .referenceValuePath("source.document$c.id")
            .paging(PagingRqDto.builder().page(0).size(100).build())
            .build();
        return Template.builder()
            .name("fallback-name")
            .format(TemplateFormat.DOCX)
            .mappings(List.of(
                TemplateMapping.builder()
                    .key("product_name")
                    .definition(
                        TemplateMappingDefinition.builder()
                            .scope(MappingScope.COLLECTION)
                            .source(referenceSource.toBuilder().path("reference.product.name").build())
                            .build()
                    )
                    .build(),
                TemplateMapping.builder()
                    .key("product_qty")
                    .definition(TemplateMappingDefinition.builder().scope(MappingScope.COLLECTION).build())
                    .build()
            ))
            .build();
    }

    private Template buildDuplicateCollectionKeyTemplate() {
        ReferenceValueSource documentReferenceSource = ReferenceValueSource.builder()
            .entityId(ENTITY_ID)
            .targetPath("source.document$c.dealProduct$c")
            .referenceFieldName("document$c")
            .referenceValuePath("source.document$c.id")
            .paging(PagingRqDto.builder().page(0).size(100).build())
            .build();
        ReferenceValueSource contractReferenceSource = ReferenceValueSource.builder()
            .entityId(ENTITY_ID)
            .targetPath("source.contract$c.dealProduct$c")
            .referenceFieldName("contract$c")
            .referenceValuePath("source.contract$c.id")
            .paging(PagingRqDto.builder().page(0).size(100).build())
            .build();
        return Template.builder()
            .name("fallback-name")
            .format(TemplateFormat.DOCX)
            .mappings(List.of(
                TemplateMapping.builder()
                    .key("product_name")
                    .definition(
                        TemplateMappingDefinition.builder()
                            .scope(MappingScope.COLLECTION)
                            .source(documentReferenceSource.toBuilder().path("reference.product.name").build())
                            .build()
                    )
                    .build(),
                TemplateMapping.builder()
                    .key("product_name")
                    .definition(
                        TemplateMappingDefinition.builder()
                            .scope(MappingScope.COLLECTION)
                            .source(contractReferenceSource.toBuilder().path("reference.product.name").build())
                            .build()
                    )
                    .build()
            ))
            .build();
    }
}
