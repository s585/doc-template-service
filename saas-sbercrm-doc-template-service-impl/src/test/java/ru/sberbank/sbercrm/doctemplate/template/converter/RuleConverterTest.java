package ru.sberbank.sbercrm.doctemplate.template.converter;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import ru.sberbank.sbercrm.doctemplate.rule.OperationRuleDto;
import ru.sberbank.sbercrm.doctemplate.rule.PrimitiveRuleDto;
import ru.sberbank.sbercrm.doctemplate.rule.RuleDto;
import ru.sberbank.sbercrm.doctemplate.template.model.rule.OperationRule;
import ru.sberbank.sbercrm.doctemplate.template.model.rule.PrimitiveRule;
import ru.sberbank.sbercrm.doctemplate.template.model.rule.Rule;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RuleConverterTest {
    private final RuleConverter converter = Mappers.getMapper(RuleConverter.class);

    @Test
    void shouldConvertOperationRuleDtoToModel() {
        OperationRuleDto dto = OperationRuleDto.builder()
            .path("source.status")
            .op("equal")
            .args(List.of(PrimitiveRuleDto.builder().value("ACTIVE").build()))
            .build();

        Rule model = converter.convertToModel(dto);

        assertThat(model).isInstanceOf(OperationRule.class);
        OperationRule operationRule = (OperationRule) model;
        assertThat(operationRule.getPath()).isEqualTo("source.status");
        assertThat(operationRule.getOp()).isEqualTo("equal");
        assertThat(operationRule.getArgs()).singleElement().isInstanceOf(PrimitiveRule.class);
    }

    @Test
    void shouldConvertOperationRuleModelToDto() {
        OperationRule model = OperationRule.builder()
            .path("source.status")
            .op("equal")
            .args(List.of(PrimitiveRule.builder().value("ACTIVE").build()))
            .build();

        RuleDto dto = converter.convertToDto(model);

        assertThat(dto).isInstanceOf(OperationRuleDto.class);
        OperationRuleDto operationRuleDto = (OperationRuleDto) dto;
        assertThat(operationRuleDto.getPath()).isEqualTo("source.status");
        assertThat(operationRuleDto.getOp()).isEqualTo("equal");
        assertThat(operationRuleDto.getArgs()).singleElement().isInstanceOf(PrimitiveRuleDto.class);
    }
}
