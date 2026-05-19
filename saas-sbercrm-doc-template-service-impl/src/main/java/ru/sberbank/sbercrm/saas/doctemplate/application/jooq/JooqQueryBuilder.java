package ru.sberbank.sbercrm.saas.doctemplate.application.jooq;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.SortField;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Component;
import ru.sberbank.sbercrm.saas.doctemplate.shared.dto.CommonRqDto;
import ru.sberbank.sbercrm.saas.doctemplate.shared.dto.FilterDto;
import ru.sberbank.sbercrm.saas.doctemplate.shared.dto.SortTypeDto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JooqQueryBuilder {
    private final ObjectMapper objectMapper;

    public Condition buildCondition(
        @Nullable Set<FilterDto> filters,
        Map<String, Field<?>> fieldMapping
    ) {
        if (filters == null || filters.isEmpty()) {
            return DSL.trueCondition();
        }

        List<Condition> conditions = filters.stream()
            .filter(Objects::nonNull)
            .map(filter -> convertToCondition(filter, fieldMapping))
            .toList();

        return conditions.isEmpty() ? DSL.trueCondition() : DSL.and(conditions);
    }

    public List<SortField<?>> buildOrderBy(
        @Nullable List<SortTypeDto> sort,
        Map<String, Field<?>> fieldMapping
    ) {
        if (sort == null || sort.isEmpty()) {
            return List.of();
        }

        List<SortField<?>> result = new ArrayList<>(sort.size());
        for (SortTypeDto item : sort) {
            Field<?> field = resolveMappedField(item.getField(), fieldMapping);
            result.add(item.getDirection() == SortTypeDto.Direction.DESC ? field.desc() : field.asc());
        }
        return result;
    }

    public int buildLimit(CommonRqDto request) {
        return request.getPaging().getSize();
    }

    public int buildOffset(CommonRqDto request) {
        return request.getPaging().getPage() * request.getPaging().getSize();
    }

    private Condition convertToCondition(
        FilterDto filter,
        Map<String, Field<?>> fieldMapping
    ) {
        if (filter.getOperation() == null) {
            throw new IllegalArgumentException("Filter operation must not be null");
        }

        return switch (filter.getOperation()) {
            case OR -> DSL.or(convertToConditions(filter.getValue(), fieldMapping));
            case AND -> DSL.and(convertToConditions(filter.getValue(), fieldMapping));
            case NOT -> DSL.not(DSL.and(convertToConditions(filter.getValue(), fieldMapping)));
            case TRUE -> booleanCondition(filter, fieldMapping, true);
            case FALSE -> booleanCondition(filter, fieldMapping, false);
            case GT -> compareCondition(filter, fieldMapping, Comparison.GT);
            case GTE -> compareCondition(filter, fieldMapping, Comparison.GTE);
            case LT -> compareCondition(filter, fieldMapping, Comparison.LT);
            case LTE -> compareCondition(filter, fieldMapping, Comparison.LTE);
            case EQUAL -> equalCondition(filter, fieldMapping);
            case NOT_EQUAL -> notEqualCondition(filter, fieldMapping);
            case EQUAL_DAY_OF_MONTH -> datePartCondition(filter, fieldMapping, "day");
            case EQUAL_MONTH -> datePartCondition(filter, fieldMapping, "month");
            case EQUAL_YEAR -> datePartCondition(filter, fieldMapping, "year");
            case CONTAINS -> likeCondition(filter, fieldMapping, MatchMode.CONTAINS);
            case CONTAINS_ANY -> containsAnyCondition(filter, fieldMapping);
            case IN -> inCondition(filter, fieldMapping, false);
            case NOT_IN -> inCondition(filter, fieldMapping, true);
            case IS_NULL -> resolveMappedField(filter.getField(), fieldMapping).isNull();
            case IS_NOT_NULL -> resolveMappedField(filter.getField(), fieldMapping).isNotNull();
            case IS_EMPTY -> emptyCondition(filter, fieldMapping, true);
            case IS_NOT_EMPTY -> emptyCondition(filter, fieldMapping, false);
            case BETWEEN -> betweenCondition(filter, fieldMapping);
            case OVERLAPS -> overlapsCondition(filter, fieldMapping);
            case IS_NULL_OR_NOT_EQUAL -> isNullOrNotEqualCondition(filter, fieldMapping);
            case STARTS_WITH -> likeCondition(filter, fieldMapping, MatchMode.STARTS_WITH);
            case ENDS_WITH -> likeCondition(filter, fieldMapping, MatchMode.ENDS_WITH);
        };
    }

    private List<Condition> convertToConditions(
        @Nullable List<Object> value,
        Map<String, Field<?>> fieldMapping
    ) {
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException("Logical filter must contain nested filters");
        }

        return value.stream()
            .map(item -> objectMapper.convertValue(item, FilterDto.class))
            .map(filter -> convertToCondition(filter, fieldMapping))
            .toList();
    }

    private Condition booleanCondition(
        FilterDto filter,
        Map<String, Field<?>> fieldMapping,
        boolean expected
    ) {
        return compareEquals(resolveMappedField(filter.getField(), fieldMapping), expected);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private Condition compareCondition(
        FilterDto filter,
        Map<String, Field<?>> fieldMapping,
        Comparison comparison
    ) {
        Field field = resolveMappedField(filter.getField(), fieldMapping);
        Object value = requireSingleValue(filter);
        Object converted = convertValue(field, value);

        return switch (comparison) {
            case GT -> field.gt(converted);
            case GTE -> field.ge(converted);
            case LT -> field.lt(converted);
            case LTE -> field.le(converted);
        };
    }

    private Condition equalCondition(FilterDto filter, Map<String, Field<?>> fieldMapping) {
        Field<?> field = resolveMappedField(filter.getField(), fieldMapping);
        return compareEquals(field, convertValue(field, requireSingleValue(filter)));
    }

    private Condition notEqualCondition(FilterDto filter, Map<String, Field<?>> fieldMapping) {
        Field<?> field = resolveMappedField(filter.getField(), fieldMapping);
        return compareNotEquals(field, convertValue(field, requireSingleValue(filter)));
    }

    private Condition datePartCondition(
        FilterDto filter,
        Map<String, Field<?>> fieldMapping,
        String part
    ) {
        Field<?> field = resolveMappedField(filter.getField(), fieldMapping);
        Integer value = objectMapper.convertValue(requireSingleValue(filter), Integer.class);
        return DSL.condition("extract(" + part + " from {0}) = {1}", field, DSL.val(value));
    }

    private Condition likeCondition(
        FilterDto filter,
        Map<String, Field<?>> fieldMapping,
        MatchMode matchMode
    ) {
        Field<String> field = resolveMappedField(filter.getField(), fieldMapping).cast(String.class);
        String value = objectMapper.convertValue(requireSingleValue(filter), String.class);
        return switch (matchMode) {
            case CONTAINS -> field.likeIgnoreCase("%" + value + "%");
            case STARTS_WITH -> field.likeIgnoreCase(value + "%");
            case ENDS_WITH -> field.likeIgnoreCase("%" + value);
        };
    }

    private Condition containsAnyCondition(
        FilterDto filter,
        Map<String, Field<?>> fieldMapping
    ) {
        if (filter.getValue() == null || filter.getValue().isEmpty()) {
            throw new IllegalArgumentException("contains_any filter must contain values");
        }

        List<Condition> conditions = filter.getValue().stream()
            .map(value -> likeCondition(
                filter.toBuilder().value(List.of(value)).build(),
                fieldMapping,
                MatchMode.CONTAINS
            ))
            .toList();
        return DSL.or(conditions);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private Condition inCondition(
        FilterDto filter,
        Map<String, Field<?>> fieldMapping,
        boolean negated
    ) {
        Field field = resolveMappedField(filter.getField(), fieldMapping);
        Collection<Object> converted = convertValues(field, filter.getValue());
        return negated ? field.notIn(converted) : field.in(converted);
    }

    private Condition emptyCondition(
        FilterDto filter,
        Map<String, Field<?>> fieldMapping,
        boolean empty
    ) {
        Field<String> field = resolveMappedField(filter.getField(), fieldMapping).cast(String.class);
        Condition condition = field.isNull().or(field.eq(""));
        return empty ? condition : condition.not();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private Condition betweenCondition(
        FilterDto filter,
        Map<String, Field<?>> fieldMapping
    ) {
        if (filter.getValue() == null || filter.getValue().size() != 2) {
            throw new IllegalArgumentException("between filter must contain exactly two values");
        }

        Field field = resolveMappedField(filter.getField(), fieldMapping);
        Object from = convertValue(field, filter.getValue().get(0));
        Object to = convertValue(field, filter.getValue().get(1));
        return field.between(from, to);
    }

    private Condition overlapsCondition(
        FilterDto filter,
        Map<String, Field<?>> fieldMapping
    ) {
        if (filter.getSecondField() == null || filter.getSecondField().isBlank()) {
            throw new IllegalArgumentException("overlaps filter requires secondField");
        }
        if (filter.getValue() == null || filter.getValue().size() != 2) {
            throw new IllegalArgumentException("overlaps filter must contain exactly two values");
        }

        Field<?> startField = resolveMappedField(filter.getField(), fieldMapping);
        Field<?> endField = resolveMappedField(filter.getSecondField(), fieldMapping);
        Object from = convertValue(startField, filter.getValue().get(0));
        Object to = convertValue(endField, filter.getValue().get(1));

        return DSL.condition("({0}, {1}) overlaps ({2}, {3})", startField, endField, DSL.val(from), DSL.val(to));
    }

    private Condition isNullOrNotEqualCondition(
        FilterDto filter,
        Map<String, Field<?>> fieldMapping
    ) {
        Field<?> field = resolveMappedField(filter.getField(), fieldMapping);
        Object value = convertValue(field, requireSingleValue(filter));
        return field.isNull().or(compareNotEquals(field, value));
    }

    private Field<?> resolveMappedField(
        @Nullable String fieldName,
        Map<String, Field<?>> fieldMapping
    ) {
        if (fieldName == null || fieldName.isBlank()) {
            throw new IllegalArgumentException("Filter field must not be blank");
        }

        Field<?> field = fieldMapping.get(fieldName);
        if (field == null) {
            throw new IllegalArgumentException("Unsupported filter field: " + fieldName);
        }
        return field;
    }

    private Object requireSingleValue(FilterDto filter) {
        if (filter.getValue() == null || filter.getValue().size() != 1) {
            throw new IllegalArgumentException(
                "Filter operation '" + filter.getOperation().value() + "' requires exactly one value"
            );
        }
        return filter.getValue().getFirst();
    }

    private Collection<Object> convertValues(Field<?> field, @Nullable Collection<Object> values) {
        if (values == null || values.isEmpty()) {
            throw new IllegalArgumentException("Filter values must not be empty");
        }
        return values.stream().map(value -> convertValue(field, value)).toList();
    }

    private Object convertValue(Field<?> field, @Nullable Object value) {
        Class<?> type = field.getType();
        if (type == null || value == null) {
            return value;
        }
        if (type.isInstance(value)) {
            return value;
        }
        if (type == UUID.class) {
            return UUID.fromString(objectMapper.convertValue(value, String.class));
        }
        if (type == OffsetDateTime.class) {
            return OffsetDateTime.parse(objectMapper.convertValue(value, String.class));
        }
        if (type == LocalDate.class) {
            return LocalDate.parse(objectMapper.convertValue(value, String.class));
        }
        if (type == BigDecimal.class) {
            return new BigDecimal(objectMapper.convertValue(value, String.class));
        }
        return objectMapper.convertValue(value, type);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private Condition compareEquals(Field<?> field, @Nullable Object value) {
        Field rawField = field;
        return rawField.eq(value);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private Condition compareNotEquals(Field<?> field, @Nullable Object value) {
        Field rawField = field;
        return rawField.ne(value);
    }

    private enum Comparison {
        GT,
        GTE,
        LT,
        LTE
    }

    private enum MatchMode {
        CONTAINS,
        STARTS_WITH,
        ENDS_WITH
    }
}
