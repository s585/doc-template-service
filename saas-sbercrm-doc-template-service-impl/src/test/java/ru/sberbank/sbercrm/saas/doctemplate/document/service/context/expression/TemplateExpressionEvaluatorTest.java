package ru.sberbank.sbercrm.saas.doctemplate.document.service.context.expression;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.sberbank.sbercrm.saas.doctemplate.application.exception.model.BusinessCrmException;
import ru.sberbank.sbercrm.saas.doctemplate.document.constant.DocumentConstants;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.MappingScope;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.TemplateMapping;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.TemplateMappingDefinition;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.TemplateValueType;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.expression.Expression;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.expression.ExpressionOperator;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.expression.OperationExpression;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.expression.PrimitiveExpression;

class TemplateExpressionEvaluatorTest {
    private final TemplateExpressionEvaluator systemUnderTest = new TemplateExpressionEvaluator();

    @Test
    @DisplayName("Evaluator возвращает source value, если expression не задан")
    void givenMissingExpression_whenEvaluate_thenReturnSourceValue() {
        Object actual = systemUnderTest.evaluate(mapping(null), "raw");

        assertThat(actual).isEqualTo("raw");
    }

    @Test
    @DisplayName("Evaluator подставляет $value и применяет строковые операции")
    void givenStringOperations_whenEvaluate_thenTransformValue() {
        Expression expression = op(
            ExpressionOperator.UPPER,
            op(ExpressionOperator.TRIM, value())
        );

        Object actual = systemUnderTest.evaluate(mapping(expression), "  customer  ");

        assertThat(actual).isEqualTo("CUSTOMER");
    }

    @Test
    @DisplayName("Evaluator выбирает первое непустое значение в coalesce")
    void givenCoalesce_whenEvaluate_thenReturnFirstNonBlankValue() {
        Expression expression = op(
            ExpressionOperator.COALESCE,
            value(),
            primitive("fallback")
        );

        Object actual = systemUnderTest.evaluate(mapping(expression), " ");

        assertThat(actual).isEqualTo("fallback");
    }

    @Test
    @DisplayName("Evaluator склеивает строковые части через concat")
    void givenConcat_whenEvaluate_thenJoinArguments() {
        Expression expression = op(
            ExpressionOperator.CONCAT,
            primitive("Contract "),
            value(),
            primitive(" / "),
            primitive(2026)
        );

        Object actual = systemUnderTest.evaluate(mapping(expression), "A-42");

        assertThat(actual).isEqualTo("Contract A-42 / 2026");
    }

    @Test
    @DisplayName("Evaluator форматирует дату")
    void givenFormatDate_whenEvaluate_thenFormatDateValue() {
        Expression expression = op(
            ExpressionOperator.FORMAT_DATE,
            value(),
            primitive("dd.MM.yyyy")
        );

        Object actual = systemUnderTest.evaluate(mapping(expression), LocalDate.of(2026, 6, 23));

        assertThat(actual).isEqualTo("23.06.2026");
    }

    @Test
    @DisplayName("Evaluator парсит ISO date string при форматировании даты")
    void givenFormatDateWithIsoString_whenEvaluate_thenFormatDateValue() {
        Expression expression = op(
            ExpressionOperator.FORMAT_DATE,
            value(),
            primitive("dd.MM.yyyy")
        );

        Object actual = systemUnderTest.evaluate(mapping(expression), "2026-06-23");

        assertThat(actual).isEqualTo("23.06.2026");
    }

    @Test
    @DisplayName("Evaluator парсит ISO local datetime string при форматировании даты")
    void givenFormatDateWithIsoLocalDateTimeString_whenEvaluate_thenFormatDateValue() {
        Expression expression = op(
            ExpressionOperator.FORMAT_DATE,
            value(),
            primitive("dd.MM.yyyy HH:mm")
        );

        Object actual = systemUnderTest.evaluate(mapping(expression), "2026-06-23T10:15:30");

        assertThat(actual).isEqualTo("23.06.2026 10:15");
    }

    @Test
    @DisplayName("Evaluator парсит ISO offset datetime string при форматировании даты")
    void givenFormatDateWithIsoOffsetDateTimeString_whenEvaluate_thenFormatDateValue() {
        Expression expression = op(
            ExpressionOperator.FORMAT_DATE,
            value(),
            primitive("dd.MM.yyyy HH:mm")
        );

        Object actual = systemUnderTest.evaluate(mapping(expression), "2026-06-23T10:15:30+03:00");

        assertThat(actual).isEqualTo("23.06.2026 10:15");
    }

    @Test
    @DisplayName("Evaluator выбрасывает business error для некорректной arity")
    void givenInvalidArity_whenEvaluate_thenThrowBusinessException() {
        Expression expression = op(ExpressionOperator.UPPER, value(), primitive("extra"));

        assertThatThrownBy(() -> systemUnderTest.evaluate(mapping(expression), "value"))
            .isInstanceOf(BusinessCrmException.class)
            .satisfies(throwable -> {
                BusinessCrmException exception = (BusinessCrmException) throwable;
                assertThat(exception.getCode()).isEqualTo(DocumentConstants.ErrorCodes.GENERATION_EXPRESSION_INVALID);
                assertThat(exception.getParams()).containsExactly("customer_name", "upper requires exactly one argument");
            });
    }

    @Test
    @DisplayName("Evaluator выбрасывает business error для некорректного формата даты")
    void givenInvalidDatePattern_whenEvaluate_thenThrowBusinessException() {
        Expression expression = op(
            ExpressionOperator.FORMAT_DATE,
            value(),
            primitive("HH:mm")
        );

        assertThatThrownBy(() -> systemUnderTest.evaluate(mapping(expression), LocalDate.of(2026, 6, 23)))
            .isInstanceOf(BusinessCrmException.class)
            .satisfies(throwable -> {
                BusinessCrmException exception = (BusinessCrmException) throwable;
                assertThat(exception.getCode()).isEqualTo(DocumentConstants.ErrorCodes.GENERATION_EXPRESSION_INVALID);
                assertThat(exception.getParams()[0]).isEqualTo("customer_name");
                assertThat(exception.getParams()[1]).asString().startsWith("formatDate failed:");
            });
    }

    private TemplateMapping mapping(Expression expression) {
        return TemplateMapping.builder()
            .key("customer_name")
            .definition(TemplateMappingDefinition.builder()
                .scope(MappingScope.VALUE)
                .type(TemplateValueType.STRING)
                .expression(expression)
                .build())
            .build();
    }

    private PrimitiveExpression value() {
        return primitive("$value");
    }

    private PrimitiveExpression primitive(Object value) {
        return PrimitiveExpression.builder().value(value).build();
    }

    private OperationExpression op(ExpressionOperator operator, Expression... args) {
        return OperationExpression.builder()
            .op(operator)
            .args(List.of(args))
            .build();
    }
}
