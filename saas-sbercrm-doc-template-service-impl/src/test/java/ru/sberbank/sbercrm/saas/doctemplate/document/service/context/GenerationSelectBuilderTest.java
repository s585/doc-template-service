package ru.sberbank.sbercrm.saas.doctemplate.document.service.context;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.sberbank.sbercrm.saas.doctemplate.shared.dto.SelectDto;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.MappingScope;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.Template;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.TemplateMapping;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.TemplateMappingDefinition;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.TemplateValueType;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.source.DirectValueSource;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.source.ReferenceValueSource;

class GenerationSelectBuilderTest {
    private final GenerationSelectBuilder systemUnderTest = new GenerationSelectBuilder(new GenerationPathResolver());

    @Test
    @DisplayName("Builder собирает select fields из direct и reference mappings")
    void givenMixedMappings_whenBuild_thenCollectRequiredSourceFields() {
        Template template = Template.builder()
            .mappings(List.of(
                TemplateMapping.builder()
                    .key("customer_name")
                    .definition(
                        TemplateMappingDefinition.builder()
                            .scope(MappingScope.VALUE)
                            .type(TemplateValueType.STRING)
                            .source(DirectValueSource.builder().path("source.customer.name").build())
                            .build()
                    )
                    .build(),
                TemplateMapping.builder()
                    .key("product_name")
                    .definition(
                        TemplateMappingDefinition.builder()
                            .scope(MappingScope.COLLECTION)
                            .type(TemplateValueType.STRING)
                            .source(
                                ReferenceValueSource.builder()
                                    .entityId(UUID.randomUUID())
                                    .targetPath("source.document$c.dealProduct$c")
                                    .referenceFieldName("document$c")
                                    .referenceValuePath("source.document$c.id")
                                    .path("reference.product.name")
                                    .build()
                            )
                            .build()
                    )
                    .build()
            ))
            .build();

        SelectDto actual = systemUnderTest.build(template);

        assertThat(actual.getFields()).isEqualTo(Set.of("customer.name", "document$c.id", "document$c.dealProduct$c"));
    }
}
