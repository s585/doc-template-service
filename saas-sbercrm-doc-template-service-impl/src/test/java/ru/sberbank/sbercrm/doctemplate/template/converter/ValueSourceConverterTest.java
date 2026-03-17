package ru.sberbank.sbercrm.doctemplate.template.converter;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import ru.sberbank.sbercrm.doctemplate.common.PagingRqDto;
import ru.sberbank.sbercrm.doctemplate.common.SortTypeDto;
import ru.sberbank.sbercrm.doctemplate.template.source.ReferenceValueSourceDto;
import ru.sberbank.sbercrm.doctemplate.template.source.ValueSourceDto;
import ru.sberbank.sbercrm.doctemplate.template.model.source.ReferenceValueSource;
import ru.sberbank.sbercrm.doctemplate.template.model.source.ValueSource;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringJUnitConfig(classes = ValueSourceConverterImpl.class)
class ValueSourceConverterTest {
    @Autowired
    private ValueSourceConverter converter;

    @Test
    void shouldConvertReferenceValueSourceDtoToModel() {
        ReferenceValueSourceDto dto = ReferenceValueSourceDto.builder()
            .targetPath("source.document$c.dealProduct$c")
            .entityId(UUID.randomUUID())
            .referenceFieldName("document$c")
            .referenceValuePath("source.document$c.id")
            .path("reference.product.name")
            .sort(List.of(SortTypeDto.builder().field("name").direction(SortTypeDto.Direction.ASC).build()))
            .paging(PagingRqDto.builder().page(0).size(100).build())
            .build();

        ValueSource model = converter.convertToModel(dto);

        assertThat(model).isInstanceOf(ReferenceValueSource.class);
        ReferenceValueSource referenceValueSource = (ReferenceValueSource) model;
        assertThat(referenceValueSource.getSort()).containsExactly(
            SortTypeDto.builder().field("name").direction(SortTypeDto.Direction.ASC).build()
        );
        assertThat(referenceValueSource.getPaging()).isEqualTo(PagingRqDto.builder().page(0).size(100).build());
    }

    @Test
    void shouldConvertReferenceValueSourceModelToDto() {
        ReferenceValueSource model = ReferenceValueSource.builder()
            .targetPath("source.document$c.dealProduct$c")
            .entityId(UUID.randomUUID())
            .referenceFieldName("document$c")
            .referenceValuePath("source.document$c.id")
            .path("reference.product.name")
            .sort(List.of(SortTypeDto.builder().field("name").direction(SortTypeDto.Direction.DESC).build()))
            .paging(PagingRqDto.builder().page(1).size(50).build())
            .build();

        ValueSourceDto dto = converter.convertToDto(model);

        assertThat(dto).isInstanceOf(ReferenceValueSourceDto.class);
        ReferenceValueSourceDto referenceValueSourceDto = (ReferenceValueSourceDto) dto;
        assertThat(referenceValueSourceDto.getSort()).singleElement()
            .extracting(SortTypeDto::getField, item -> item.getDirection().value())
            .containsExactly("name", "DESC");
        assertThat(referenceValueSourceDto.getPaging().getPage()).isEqualTo(1);
        assertThat(referenceValueSourceDto.getPaging().getSize()).isEqualTo(50);
    }
}
