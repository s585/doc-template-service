package ru.sberbank.sbercrm.saas.doctemplate.application.jooq;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jooq.JSONB;
import org.junit.jupiter.api.Test;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.MappingScope;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.TemplateMappingDefinition;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.TemplateValueType;
import ru.sberbank.sbercrm.doctemplate.shared.dto.FilterDto;
import ru.sberbank.sbercrm.doctemplate.shared.dto.PagingRqDto;
import ru.sberbank.sbercrm.doctemplate.shared.dto.SortTypeDto;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.source.ReferenceValueSource;

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
    void shouldRoundTripFilter() {
        FilterDto model = FilterDto.builder()
            .field("source.status")
            .operation(FilterDto.Operation.EQUAL)
            .value(List.of("ACTIVE"))
            .build();

        JSONB jsonb = jsonbHelper.toJsonb(model);
        FilterDto restored = jsonbHelper.fromJsonb(jsonb, FilterDto.class);

        assertThat(restored).isEqualTo(model);
    }
}
