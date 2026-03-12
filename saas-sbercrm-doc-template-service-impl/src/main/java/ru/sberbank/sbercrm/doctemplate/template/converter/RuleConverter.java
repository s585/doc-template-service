package ru.sberbank.sbercrm.doctemplate.template.converter;

import org.mapstruct.Mapper;
import ru.sberbank.sbercrm.doctemplate.rule.OperationRuleDto;
import ru.sberbank.sbercrm.doctemplate.rule.PrimitiveRuleDto;
import ru.sberbank.sbercrm.doctemplate.rule.RuleDto;
import ru.sberbank.sbercrm.doctemplate.template.model.rule.OperationRule;
import ru.sberbank.sbercrm.doctemplate.template.model.rule.PrimitiveRule;
import ru.sberbank.sbercrm.doctemplate.template.model.rule.Rule;

@Mapper(componentModel = "spring")
public interface RuleConverter {
    default Rule convertToModel(RuleDto dto) {
        return switch (dto) {
            case null -> null;
            case OperationRuleDto operationRuleDto -> convertOperationRuleToModel(operationRuleDto);
            case PrimitiveRuleDto primitiveRuleDto -> convertPrimitiveRuleToModel(primitiveRuleDto);
            default -> throw new IllegalArgumentException("Unsupported rule dto type: " + dto.getClass().getName());
        };
    }

    default RuleDto convertToDto(Rule model) {
        return switch (model) {
            case null -> null;
            case OperationRule operationRule -> convertOperationRuleToDto(operationRule);
            case PrimitiveRule primitiveRule -> convertPrimitiveRuleToDto(primitiveRule);
        };
    }

    OperationRule convertOperationRuleToModel(OperationRuleDto dto);

    PrimitiveRule convertPrimitiveRuleToModel(PrimitiveRuleDto dto);

    OperationRuleDto convertOperationRuleToDto(OperationRule model);

    PrimitiveRuleDto convertPrimitiveRuleToDto(PrimitiveRule model);
}
