package ru.sberbank.sbercrm.doctemplate.shared.dto;


import com.fasterxml.jackson.annotation.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.Serializable;
import java.util.*;

@JsonPropertyOrder({"select", "filter", "sort", "paging", "itemsPerPage", "additionalItems"})
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class CommonRqDto implements Serializable {
    private static final long serialVersionUID = 7490832299763596282L;
    @Valid
    @Builder.Default
    private final Map<String, Object> additionalProperties = new HashMap<>();
    /**
     * Список полей для получения в ответе
     */
    private SelectDto select;
    /**
     * Фильтр.
     */
    @Valid
    @Builder.Default
    @JsonSetter(nulls = Nulls.SKIP)
    private Set<FilterDto> filter = new HashSet<>();
    /**
     * Сортировка.
     */
    @Valid
    @Builder.Default
    @JsonSetter(nulls = Nulls.SKIP)
    private List<SortTypeDto> sort = new ArrayList<>();
    /**
     * (Required).
     */
    @Valid
    @NotNull
    private PagingRqDto paging;
    /**
     * Количество элементов на странице.
     */
    private Long itemsPerPage;
    /**
     * Флаги для выборки данных, см. CommonRqFlagEnum
     */
    private Set<String> flags;
    private Object additionalItems;
}
