package ru.sberbank.sbercrm.doctemplate.template.converter;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.sberbank.sbercrm.doctemplate.expression.ExpressionDto;
import ru.sberbank.sbercrm.doctemplate.expression.OperationExpressionDto;
import ru.sberbank.sbercrm.doctemplate.expression.PrimitiveExpressionDto;
import ru.sberbank.sbercrm.doctemplate.template.model.expression.Expression;
import ru.sberbank.sbercrm.doctemplate.template.model.expression.ExpressionOperator;
import ru.sberbank.sbercrm.doctemplate.template.model.expression.OperationExpression;
import ru.sberbank.sbercrm.doctemplate.template.model.expression.PrimitiveExpression;

@Mapper(componentModel = "spring")
public interface ExpressionConverter {
    default Expression convertToModel(ExpressionDto dto) {
        return switch (dto) {
            case null -> null;
            case OperationExpressionDto operationExpressionDto ->
                convertOperationExpressionToModel(operationExpressionDto);
            case PrimitiveExpressionDto primitiveExpressionDto ->
                convertPrimitiveExpressionToModel(primitiveExpressionDto);
            default ->
                throw new IllegalArgumentException("Unsupported expression dto type: " + dto.getClass().getName());
        };
    }

    default ExpressionDto convertToDto(Expression model) {
        return switch (model) {
            case null -> null;
            case OperationExpression operationExpression ->
                convertOperationExpressionToDto(operationExpression);
            case PrimitiveExpression primitiveExpression ->
                convertPrimitiveExpressionToDto(primitiveExpression);
        };
    }

    @Mapping(target = "op", expression = "java(convertToOperator(dto.getOp()))")
    OperationExpression convertOperationExpressionToModel(OperationExpressionDto dto);

    PrimitiveExpression convertPrimitiveExpressionToModel(PrimitiveExpressionDto dto);

    @Mapping(target = "op", expression = "java(convertToOperatorValue(model.getOp()))")
    OperationExpressionDto convertOperationExpressionToDto(OperationExpression model);

    PrimitiveExpressionDto convertPrimitiveExpressionToDto(PrimitiveExpression model);

    default ExpressionOperator convertToOperator(String value) {
        return value == null ? null : ExpressionOperator.fromValue(value);
    }

    default String convertToOperatorValue(ExpressionOperator value) {
        return value == null ? null : value.value();
    }
}
