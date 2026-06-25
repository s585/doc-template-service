package ru.sberbank.sbercrm.saas.doctemplate.document.service.context.expression;

import java.time.Instant;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Date;
import java.util.List;
import org.springframework.stereotype.Service;
import ru.sberbank.sbercrm.saas.doctemplate.application.exception.model.BusinessCrmException;
import ru.sberbank.sbercrm.saas.doctemplate.document.constant.DocumentConstants;
import ru.sberbank.sbercrm.saas.doctemplate.document.service.context.expression.util.TemplateExpressionDateParser;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.TemplateMapping;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.expression.Expression;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.expression.OperationExpression;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.expression.PrimitiveExpression;

@Service
public class TemplateExpressionEvaluator implements ExpressionEvaluator {
    private static final String VALUE_REFERENCE = "$value";

    @Override
    public Object evaluate(TemplateMapping mapping, Object sourceValue) {
        if (mapping == null || mapping.getDefinition() == null || mapping.getDefinition().getExpression() == null) {
            return sourceValue;
        }
        return evaluateExpression(mapping.getDefinition().getExpression(), sourceValue, mapping.getKey());
    }

    private Object evaluateExpression(Expression expression, Object sourceValue, String mappingKey) {
        return switch (expression) {
            case PrimitiveExpression primitiveExpression -> evaluatePrimitive(primitiveExpression, sourceValue);
            case OperationExpression operationExpression -> evaluateOperation(operationExpression, sourceValue, mappingKey);
        };
    }

    private Object evaluatePrimitive(PrimitiveExpression expression, Object sourceValue) {
        Object value = expression.getValue();
        return VALUE_REFERENCE.equals(value) ? sourceValue : value;
    }

    private Object evaluateOperation(OperationExpression expression, Object sourceValue, String mappingKey) {
        if (expression.getOp() == null) {
            throw invalid(mappingKey, "operator is required");
        }
        List<Expression> args = expression.getArgs() == null ? List.of() : expression.getArgs();
        return switch (expression.getOp()) {
            case COALESCE -> coalesce(args, sourceValue, mappingKey);
            case CONCAT -> concat(args, sourceValue, mappingKey);
            case FORMAT_DATE -> formatDate(args, sourceValue, mappingKey);
            case UPPER -> toText(singleArg(args, sourceValue, mappingKey, "upper")).toUpperCase();
            case LOWER -> toText(singleArg(args, sourceValue, mappingKey, "lower")).toLowerCase();
            case TRIM -> toText(singleArg(args, sourceValue, mappingKey, "trim")).trim();
        };
    }

    private Object coalesce(List<Expression> args, Object sourceValue, String mappingKey) {
        if (args.isEmpty()) {
            throw invalid(mappingKey, "coalesce requires at least one argument");
        }
        for (Expression arg : args) {
            Object value = evaluateExpression(arg, sourceValue, mappingKey);
            if (!isEmpty(value)) {
                return value;
            }
        }
        return null;
    }

    private String concat(List<Expression> args, Object sourceValue, String mappingKey) {
        StringBuilder result = new StringBuilder();
        for (Expression arg : args) {
            result.append(toText(evaluateExpression(arg, sourceValue, mappingKey)));
        }
        return result.toString();
    }

    private String formatDate(List<Expression> args, Object sourceValue, String mappingKey) {
        if (args.size() != 2) {
            throw invalid(mappingKey, "formatDate requires value and output pattern arguments");
        }
        Object value = evaluateExpression(args.get(0), sourceValue, mappingKey);
        if (isEmpty(value)) {
            return "";
        }
        Object pattern = evaluateExpression(args.get(1), sourceValue, mappingKey);
        if (isEmpty(pattern)) {
            throw invalid(mappingKey, "formatDate output pattern is required");
        }
        TemporalAccessor temporal = toTemporal(value, mappingKey);
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(toText(pattern));
            return formatter.format(temporal);
        } catch (IllegalArgumentException | DateTimeException exception) {
            throw invalid(mappingKey, "formatDate failed: " + exception.getMessage());
        }
    }

    private Object singleArg(List<Expression> args, Object sourceValue, String mappingKey, String operator) {
        if (args.size() != 1) {
            throw invalid(mappingKey, operator + " requires exactly one argument");
        }
        return evaluateExpression(args.getFirst(), sourceValue, mappingKey);
    }

    private TemporalAccessor toTemporal(Object value, String mappingKey) {
        if (value instanceof LocalDate localDate) {
            return localDate;
        }
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime;
        }
        if (value instanceof OffsetDateTime offsetDateTime) {
            return offsetDateTime;
        }
        if (value instanceof ZonedDateTime zonedDateTime) {
            return zonedDateTime;
        }
        if (value instanceof Instant instant) {
            return instant.atZone(ZoneId.systemDefault());
        }
        if (value instanceof Date date) {
            return date.toInstant().atZone(ZoneId.systemDefault());
        }
        if (value instanceof CharSequence charSequence) {
            return TemplateExpressionDateParser.parse(charSequence.toString(), mappingKey);
        }
        throw invalid(mappingKey, "formatDate value has unsupported type: " + value.getClass().getSimpleName());
    }

    private boolean isEmpty(Object value) {
        return value == null || value instanceof CharSequence charSequence && charSequence.toString().isBlank();
    }

    private String toText(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private BusinessCrmException invalid(String mappingKey, String reason) {
        return new BusinessCrmException(
            DocumentConstants.ErrorCodes.GENERATION_EXPRESSION_INVALID,
            DocumentConstants.ErrorCodes.GENERATION_EXPRESSION_INVALID,
            mappingKey,
            reason
        );
    }
}
