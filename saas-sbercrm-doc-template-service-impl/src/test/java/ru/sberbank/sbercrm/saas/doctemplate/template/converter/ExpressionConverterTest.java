package ru.sberbank.sbercrm.saas.doctemplate.template.converter;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import ru.sberbank.sbercrm.saas.doctemplate.template.dto.expression.ExpressionDto;
import ru.sberbank.sbercrm.saas.doctemplate.template.dto.expression.OperationExpressionDto;
import ru.sberbank.sbercrm.saas.doctemplate.template.dto.expression.PrimitiveExpressionDto;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.expression.Expression;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.expression.ExpressionOperator;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.expression.OperationExpression;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.expression.PrimitiveExpression;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ExpressionConverterTest {
    private final ExpressionConverter converter = Mappers.getMapper(ExpressionConverter.class);

    @Test
    void shouldMapOperationExpressionDtoToModel() {
        OperationExpressionDto dto = OperationExpressionDto.builder()
            .op("formatDate")
            .args(List.of(PrimitiveExpressionDto.builder().value("$value").build()))
            .build();

        Expression model = converter.convertToModel(dto);

        assertThat(model).isInstanceOf(OperationExpression.class);
        OperationExpression operationExpression = (OperationExpression) model;
        assertThat(operationExpression.getOp()).isEqualTo(ExpressionOperator.FORMAT_DATE);
        assertThat(operationExpression.getArgs()).singleElement().isInstanceOf(PrimitiveExpression.class);
        assertThat(((PrimitiveExpression) operationExpression.getArgs().getFirst()).getValue()).isEqualTo("$value");
    }

    @Test
    void shouldMapOperationExpressionModelToDto() {
        OperationExpression model = OperationExpression.builder()
            .op(ExpressionOperator.COALESCE)
            .args(List.of(
                PrimitiveExpression.builder().value("$value").build(),
                PrimitiveExpression.builder().value("-").build()
            ))
            .build();

        ExpressionDto dto = converter.convertToDto(model);

        assertThat(dto).isInstanceOf(OperationExpressionDto.class);
        OperationExpressionDto operationExpressionDto = (OperationExpressionDto) dto;
        assertThat(operationExpressionDto.getOp()).isEqualTo("coalesce");
        assertThat(operationExpressionDto.getArgs()).hasSize(2);
        assertThat(((PrimitiveExpressionDto) operationExpressionDto.getArgs().getFirst()).getValue()).isEqualTo("$value");
        assertThat(((PrimitiveExpressionDto) operationExpressionDto.getArgs().get(1)).getValue()).isEqualTo("-");
    }
}
