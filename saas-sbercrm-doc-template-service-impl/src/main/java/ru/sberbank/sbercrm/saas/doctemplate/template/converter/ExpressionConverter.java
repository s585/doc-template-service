package ru.sberbank.sbercrm.saas.doctemplate.template.converter;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import ru.sberbank.sbercrm.saas.doctemplate.template.dto.expression.ExpressionDto;
import ru.sberbank.sbercrm.saas.doctemplate.template.dto.expression.OperationExpressionDto;
import ru.sberbank.sbercrm.saas.doctemplate.template.dto.expression.PrimitiveExpressionDto;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.expression.Expression;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.expression.ExpressionOperator;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.expression.OperationExpression;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.expression.PrimitiveExpression;

@Mapper(componentModel = "spring")
public interface ExpressionConverter {
    @Nullable
    default Expression convertToModel(@Nullable ExpressionDto dto) {
        if (dto == null) {
            return null;
        }
        return switch (dto) {
            case OperationExpressionDto operationExpressionDto ->
                convertOperationExpressionToModel(operationExpressionDto);
            case PrimitiveExpressionDto primitiveExpressionDto ->
                convertPrimitiveExpressionToModel(primitiveExpressionDto);
            default ->
                throw new IllegalArgumentException("Unsupported expression dto type: " + dto.getClass().getName());
        };
    }

    @Nullable
    default ExpressionDto convertToDto(@Nullable Expression model) {
        return switch (model) {
            case null -> null;
            case OperationExpression operationExpression ->
                convertOperationExpressionToDto(operationExpression);
            case PrimitiveExpression primitiveExpression ->
                convertPrimitiveExpressionToDto(primitiveExpression);
        };
    }

    @Mapping(target = "op", source = "op", qualifiedByName = "convertToOperator")
    OperationExpression convertOperationExpressionToModel(OperationExpressionDto dto);

    PrimitiveExpression convertPrimitiveExpressionToModel(PrimitiveExpressionDto dto);

    @Mapping(target = "op", source = "op", qualifiedByName = "convertToOperatorValue")
    OperationExpressionDto convertOperationExpressionToDto(OperationExpression model);

    PrimitiveExpressionDto convertPrimitiveExpressionToDto(PrimitiveExpression model);

    @Named("convertToOperator")
    @Nullable
    default ExpressionOperator convertToOperator(@Nullable String value) {
        return value == null ? null : ExpressionOperator.fromValue(value);
    }

    @Named("convertToOperatorValue")
    @Nullable
    default String convertToOperatorValue(@Nullable ExpressionOperator value) {
        return value == null ? null : value.value();
    }
}
