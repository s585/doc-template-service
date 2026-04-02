package ru.sberbank.sbercrm.saas.doctemplate.application.jooq;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.SQLDialect;
import org.jooq.SortField;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import ru.sberbank.sbercrm.saas.doctemplate.shared.dto.CommonRqDto;
import ru.sberbank.sbercrm.saas.doctemplate.shared.dto.FilterDto;
import ru.sberbank.sbercrm.saas.doctemplate.shared.dto.PagingRqDto;
import ru.sberbank.sbercrm.saas.doctemplate.shared.dto.SortTypeDto;

class JooqQueryBuilderTest {
    private static final UUID ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final OffsetDateTime TIMESTAMP = OffsetDateTime.parse("2026-03-25T10:15:30+03:00");
    private static final LocalDate DATE = LocalDate.parse("2026-03-25");

    private final JooqQueryBuilder queryBuilder = new JooqQueryBuilder(new ObjectMapper().findAndRegisterModules());
    private final Map<String, Field<?>> fieldMapping = Map.of(
        "amount", DSL.field(DSL.name("amount"), BigDecimal.class),
        "active", DSL.field(DSL.name("active"), Boolean.class),
        "name", DSL.field(DSL.name("name"), String.class),
        "id", DSL.field(DSL.name("id"), UUID.class),
        "createdAt", DSL.field(DSL.name("created_at"), OffsetDateTime.class),
        "eventDate", DSL.field(DSL.name("event_date"), LocalDate.class),
        "startAt", DSL.field(DSL.name("start_at"), OffsetDateTime.class),
        "endAt", DSL.field(DSL.name("end_at"), OffsetDateTime.class)
    );

    @Test
    @DisplayName("Построитель возвращает true condition, если фильтры не заданы")
    void givenNoFilters_whenBuildCondition_thenReturnTrueCondition() {
        // given

        // when
        Condition condition = queryBuilder.buildCondition(null, fieldMapping);

        // then
        assertThat(render(condition)).isEqualTo("true");
    }

    @Test
    @DisplayName("Построитель собирает условия сравнения, логики и поиска по строкам")
    void givenDifferentFilterOperations_whenBuildCondition_thenRenderExpectedSql() {
        // given
        FilterDto amountFilter = FilterDto.builder()
            .field("amount")
            .operation(FilterDto.Operation.GTE)
            .value(List.of("10.50"))
            .build();
        FilterDto activeFilter = FilterDto.builder()
            .field("active")
            .operation(FilterDto.Operation.TRUE)
            .value(List.of())
            .build();
        FilterDto containsFilter = FilterDto.builder()
            .field("name")
            .operation(FilterDto.Operation.CONTAINS)
            .value(List.of("contract"))
            .build();
        FilterDto startsWithFilter = FilterDto.builder()
            .field("name")
            .operation(FilterDto.Operation.STARTS_WITH)
            .value(List.of("SUP"))
            .build();
        FilterDto endsWithFilter = FilterDto.builder()
            .field("name")
            .operation(FilterDto.Operation.ENDS_WITH)
            .value(List.of("TRACT"))
            .build();
        FilterDto logicalFilter = FilterDto.builder()
            .operation(FilterDto.Operation.OR)
            .value(List.of(amountFilter, containsFilter))
            .build();

        // when
        Condition condition = queryBuilder.buildCondition(
            Set.of(activeFilter, startsWithFilter, endsWithFilter, logicalFilter),
            fieldMapping
        );

        // then
        String sql = render(condition);
        assertThat(sql)
            .contains("\"active\" = true")
            .contains("\"amount\" >= 10.50")
            .contains("cast(\"name\" as varchar) ilike '%contract%'")
            .contains("cast(\"name\" as varchar) ilike 'SUP%'")
            .contains("cast(\"name\" as varchar) ilike '%TRACT'")
            .contains(" or ");
    }

    @Test
    @DisplayName("Построитель поддерживает списковые, диапазонные и специальные фильтры")
    void givenSpecialFilterOperations_whenBuildCondition_thenRenderExpectedSql() {
        // given
        FilterDto inFilter = FilterDto.builder()
            .field("id")
            .operation(FilterDto.Operation.IN)
            .value(List.of(ID.toString()))
            .build();
        FilterDto notInFilter = FilterDto.builder()
            .field("id")
            .operation(FilterDto.Operation.NOT_IN)
            .value(List.of(ID.toString()))
            .build();
        FilterDto betweenFilter = FilterDto.builder()
            .field("amount")
            .operation(FilterDto.Operation.BETWEEN)
            .value(List.of("1.5", "9.5"))
            .build();
        FilterDto overlapsFilter = FilterDto.builder()
            .field("startAt")
            .secondField("endAt")
            .operation(FilterDto.Operation.OVERLAPS)
            .value(List.of(TIMESTAMP.toString(), TIMESTAMP.plusDays(1).toString()))
            .build();
        FilterDto containsAnyFilter = FilterDto.builder()
            .field("name")
            .operation(FilterDto.Operation.CONTAINS_ANY)
            .value(List.of("alpha", "beta"))
            .build();
        FilterDto emptyFilter = FilterDto.builder()
            .field("name")
            .operation(FilterDto.Operation.IS_EMPTY)
            .value(List.of())
            .build();
        FilterDto notEmptyFilter = FilterDto.builder()
            .field("name")
            .operation(FilterDto.Operation.IS_NOT_EMPTY)
            .value(List.of())
            .build();
        FilterDto nullOrNotEqualFilter = FilterDto.builder()
            .field("name")
            .operation(FilterDto.Operation.IS_NULL_OR_NOT_EQUAL)
            .value(List.of("draft"))
            .build();

        // when
        Condition condition = queryBuilder.buildCondition(
            Set.of(
                inFilter,
                notInFilter,
                betweenFilter,
                overlapsFilter,
                containsAnyFilter,
                emptyFilter,
                notEmptyFilter,
                nullOrNotEqualFilter
            ),
            fieldMapping
        );

        // then
        String sql = render(condition);
        assertThat(sql)
            .contains("\"id\" in (cast('11111111-1111-1111-1111-111111111111' as uuid))")
            .contains("\"id\" not in (cast('11111111-1111-1111-1111-111111111111' as uuid))")
            .contains("\"amount\" between 1.5 and 9.5")
            .contains("overlaps")
            .contains("alpha")
            .contains("beta")
            .contains("cast(\"name\" as varchar) is null or cast(\"name\" as varchar) = ''")
            .contains("not (cast(\"name\" as varchar) is null or cast(\"name\" as varchar) = '')")
            .contains("\"name\" is null or \"name\" <> 'draft'");
    }

    @Test
    @DisplayName("Построитель поддерживает даты, null-проверки и отрицание")
    void givenDateAndLogicalFilters_whenBuildCondition_thenRenderExpectedSql() {
        // given
        FilterDto equalDateFilter = FilterDto.builder()
            .field("eventDate")
            .operation(FilterDto.Operation.EQUAL)
            .value(List.of(DATE.toString()))
            .build();
        FilterDto monthFilter = FilterDto.builder()
            .field("eventDate")
            .operation(FilterDto.Operation.EQUAL_MONTH)
            .value(List.of(3))
            .build();
        FilterDto dayFilter = FilterDto.builder()
            .field("eventDate")
            .operation(FilterDto.Operation.EQUAL_DAY_OF_MONTH)
            .value(List.of(25))
            .build();
        FilterDto yearFilter = FilterDto.builder()
            .field("eventDate")
            .operation(FilterDto.Operation.EQUAL_YEAR)
            .value(List.of(2026))
            .build();
        FilterDto notEqualFilter = FilterDto.builder()
            .field("name")
            .operation(FilterDto.Operation.NOT_EQUAL)
            .value(List.of("archived"))
            .build();
        FilterDto isNullFilter = FilterDto.builder()
            .field("name")
            .operation(FilterDto.Operation.IS_NULL)
            .value(List.of())
            .build();
        FilterDto isNotNullFilter = FilterDto.builder()
            .field("name")
            .operation(FilterDto.Operation.IS_NOT_NULL)
            .value(List.of())
            .build();
        FilterDto notFilter = FilterDto.builder()
            .operation(FilterDto.Operation.NOT)
            .value(List.of(equalDateFilter))
            .build();

        // when
        Condition condition = queryBuilder.buildCondition(
            Set.of(monthFilter, dayFilter, yearFilter, notEqualFilter, isNullFilter, isNotNullFilter, notFilter),
            fieldMapping
        );

        // then
        String sql = render(condition);
        assertThat(sql)
            .contains("extract(month from \"event_date\") = 3")
            .contains("extract(day from \"event_date\") = 25")
            .contains("extract(year from \"event_date\") = 2026")
            .contains("\"name\" <> 'archived'")
            .contains("\"name\" is null")
            .contains("\"name\" is not null")
            .contains("not (\"event_date\" = date '2026-03-25')");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidFilterCases")
    @DisplayName("Построитель выбрасывает ошибку для невалидных фильтров")
    void givenInvalidFilters_whenBuildCondition_thenThrowIllegalArgumentException(
        String caseName,
        Set<FilterDto> filters,
        String expectedMessagePart
    ) {
        assertThatThrownBy(() -> queryBuilder.buildCondition(filters, fieldMapping))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining(expectedMessagePart);
    }

    @Test
    @DisplayName("Построитель формирует сортировку, лимит и смещение")
    void givenCommonRequest_whenBuildOrderByLimitAndOffset_thenReturnExpectedValues() {
        // given
        List<SortTypeDto> sort = List.of(
            SortTypeDto.builder().field("name").direction(SortTypeDto.Direction.ASC).build(),
            SortTypeDto.builder().field("createdAt").direction(SortTypeDto.Direction.DESC).build()
        );
        CommonRqDto request = CommonRqDto.builder()
            .paging(PagingRqDto.builder().page(2).size(25).build())
            .build();

        // when
        List<SortField<?>> orderBy = queryBuilder.buildOrderBy(sort, fieldMapping);
        int limit = queryBuilder.buildLimit(request);
        int offset = queryBuilder.buildOffset(request);

        // then
        assertThat(orderBy).hasSize(2);
        assertThat(render(orderBy.getFirst())).isEqualTo("\"name\" asc");
        assertThat(render(orderBy.get(1))).isEqualTo("\"created_at\" desc");
        assertThat(limit).isEqualTo(25);
        assertThat(offset).isEqualTo(50);
    }

    private String render(Condition condition) {
        return DSL.using(SQLDialect.POSTGRES).renderInlined(condition);
    }

    private String render(SortField<?> sortField) {
        return DSL.using(SQLDialect.POSTGRES).render(sortField);
    }

    private static Stream<Arguments> invalidFilterCases() {
        FilterDto unsupportedFieldFilter = FilterDto.builder()
            .field("unknown")
            .operation(FilterDto.Operation.EQUAL)
            .value(List.of("value"))
            .build();
        FilterDto blankFieldFilter = FilterDto.builder()
            .field(" ")
            .operation(FilterDto.Operation.EQUAL)
            .value(List.of("value"))
            .build();
        FilterDto noOperationFilter = FilterDto.builder()
            .field("name")
            .value(List.of("value"))
            .build();
        FilterDto invalidValueCountFilter = FilterDto.builder()
            .field("name")
            .operation(FilterDto.Operation.EQUAL)
            .value(List.of("a", "b"))
            .build();
        FilterDto emptyContainsAnyFilter = FilterDto.builder()
            .field("name")
            .operation(FilterDto.Operation.CONTAINS_ANY)
            .value(List.of())
            .build();
        FilterDto invalidBetweenFilter = FilterDto.builder()
            .field("amount")
            .operation(FilterDto.Operation.BETWEEN)
            .value(List.of("1"))
            .build();
        FilterDto invalidOverlapsFilter = FilterDto.builder()
            .field("startAt")
            .operation(FilterDto.Operation.OVERLAPS)
            .value(List.of(TIMESTAMP.toString(), TIMESTAMP.plusDays(1).toString()))
            .build();
        FilterDto emptyLogicalFilter = FilterDto.builder()
            .operation(FilterDto.Operation.AND)
            .value(List.of())
            .build();

        return Stream.of(
            Arguments.of("unsupported field", Set.of(unsupportedFieldFilter), "Unsupported filter field"),
            Arguments.of("blank field", Set.of(blankFieldFilter), "must not be blank"),
            Arguments.of("missing operation", Set.of(noOperationFilter), "must not be null"),
            Arguments.of("invalid scalar value count", Set.of(invalidValueCountFilter), "requires exactly one value"),
            Arguments.of("empty contains_any", Set.of(emptyContainsAnyFilter), "contains_any filter must contain values"),
            Arguments.of("invalid between", Set.of(invalidBetweenFilter), "between filter must contain exactly two values"),
            Arguments.of("invalid overlaps", Set.of(invalidOverlapsFilter), "requires secondField"),
            Arguments.of("empty logical filter", Set.of(emptyLogicalFilter), "must contain nested filters")
        );
    }
}
