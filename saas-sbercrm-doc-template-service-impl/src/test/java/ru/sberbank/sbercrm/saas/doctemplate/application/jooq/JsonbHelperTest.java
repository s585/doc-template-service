package ru.sberbank.sbercrm.saas.doctemplate.application.jooq;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jooq.JSONB;
import org.junit.jupiter.api.Test;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.MappingScope;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.TemplateMappingDefinition;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.TemplateValueType;
import ru.sberbank.sbercrm.saas.doctemplate.shared.dto.FilterDto;
import ru.sberbank.sbercrm.saas.doctemplate.shared.dto.PagingRqDto;
import ru.sberbank.sbercrm.saas.doctemplate.shared.dto.SortTypeDto;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.source.ReferenceValueSource;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JsonbHelperTest {
    private static final UUID ENTITY_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private final JsonbHelper systemUnderTest = new JsonbHelper(new ObjectMapper());

    @Test
    void shouldRoundTripTemplateMappingDefinition() {
        TemplateMappingDefinition model = TemplateMappingDefinition.builder()
            .scope(MappingScope.COLLECTION)
            .type(TemplateValueType.STRING)
            .source(ReferenceValueSource.builder()
                .targetPath("source.document$c.dealProduct$c")
                .entityId(ENTITY_ID)
                .referenceFieldName("document$c")
                .referenceValuePath("source.document$c.id")
                .path("reference.product.name")
                .sort(List.of(SortTypeDto.builder().field("name").direction(SortTypeDto.Direction.ASC).build()))
                .paging(PagingRqDto.builder().page(0).size(100).build())
                .build())
            .build();

        JSONB jsonb = systemUnderTest.toJsonb(model);
        TemplateMappingDefinition restored = systemUnderTest.fromJsonb(jsonb, TemplateMappingDefinition.class);

        assertThat(restored).isEqualTo(model);
    }

    @Test
    void shouldRoundTripFilter() {
        FilterDto model = FilterDto.builder()
            .field("source.status")
            .operation(FilterDto.Operation.EQUAL)
            .value(List.of("ACTIVE"))
            .build();

        JSONB jsonb = systemUnderTest.toJsonb(model);
        FilterDto restored = systemUnderTest.fromJsonb(jsonb, FilterDto.class);

        assertThat(restored).isEqualTo(model);
    }
}
