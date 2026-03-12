package ru.sberbank.sbercrm.doctemplate.common.helper;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jooq.JSONB;
import org.junit.jupiter.api.Test;
import ru.sberbank.sbercrm.doctemplate.template.model.MappingScope;
import ru.sberbank.sbercrm.doctemplate.template.model.TemplateMappingDefinition;
import ru.sberbank.sbercrm.doctemplate.template.model.TemplateValueType;
import ru.sberbank.sbercrm.doctemplate.common.PagingRqDto;
import ru.sberbank.sbercrm.doctemplate.common.SortTypeDto;
import ru.sberbank.sbercrm.doctemplate.template.model.rule.OperationRule;
import ru.sberbank.sbercrm.doctemplate.template.model.rule.PrimitiveRule;
import ru.sberbank.sbercrm.doctemplate.template.model.rule.Rule;
import ru.sberbank.sbercrm.doctemplate.template.model.source.ReferenceValueSource;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JsonbHelperTest {
    private final JsonbHelper jsonbHelper = new JsonbHelper(new ObjectMapper());

    @Test
    void shouldRoundTripTemplateMappingDefinition() {
        TemplateMappingDefinition model = TemplateMappingDefinition.builder()
            .scope(MappingScope.TABLE)
            .type(TemplateValueType.STRING)
            .source(ReferenceValueSource.builder()
                .targetPath("source.document$c.dealProduct$c")
                .entityId(UUID.randomUUID())
                .referenceFieldName("document$c")
                .referenceValuePath("source.document$c.id")
                .path("reference.product.name")
                .sort(List.of(SortTypeDto.builder().field("name").direction(SortTypeDto.Direction.ASC).build()))
                .paging(PagingRqDto.builder().page(0).size(100).build())
                .build())
            .build();

        JSONB jsonb = jsonbHelper.toJsonb(model);
        TemplateMappingDefinition restored = jsonbHelper.fromJsonb(jsonb, TemplateMappingDefinition.class);

        assertThat(restored).isEqualTo(model);
    }

    @Test
    void shouldRoundTripRule() {
        Rule model = OperationRule.builder()
            .path("source.status")
            .op("equal")
            .args(List.of(PrimitiveRule.builder().value("ACTIVE").build()))
            .build();

        JSONB jsonb = jsonbHelper.toJsonb(model);
        Rule restored = jsonbHelper.fromJsonb(jsonb, Rule.class);

        assertThat(restored).isEqualTo(model);
    }
}
