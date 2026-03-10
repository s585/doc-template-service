package ru.sberbank.sbercrm.doctemplate.template.source;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.sberbank.sbercrm.doctemplate.common.PagingRqDto;
import ru.sberbank.sbercrm.doctemplate.common.SortTypeDto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static ru.sberbank.sbercrm.doctemplate.template.source.SourceDto.SourceKind.REFERENCE;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@JsonTypeName(REFERENCE)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder({"kind", "targetPath", "entityId", "referenceFieldName", "referenceValuePath", "path", "sort", "paging"})
@Schema(description = "Источник значения, которое загружается отдельным запросом по обратной ссылке")
public class ReferenceSourceDto implements SourceDto, Serializable {
    private static final long serialVersionUID = 6995429950117882487L;

    @NotBlank
    @Schema(description = "Путь обогащаемого узла исходного объекта", example = "source.document$c.dealProduct$c")
    private String targetPath;

    @NotNull
    @Schema(description = "Идентификатор сущности, по которой выполняется reference-запрос")
    private UUID entityId;

    @NotBlank
    @Schema(description = "Имя поля в целевой сущности, по которому строится фильтр")
    private String referenceFieldName;

    @NotBlank
    @Schema(description = "Путь в исходном объекте, откуда берется значение для фильтра reference-запроса", example = "source.document$c.id")
    private String referenceValuePath;

    @NotBlank
    @Schema(description = "Путь до итогового значения внутри reference-объекта", example = "reference.product.name")
    private String path;

    @Valid
    @Builder.Default
    @Schema(description = "Сортировка reference-запроса")
    private List<SortTypeDto> sort = new ArrayList<>();

    @Valid
    @NotNull
    @Schema(description = "Параметры пагинации reference-запроса")
    private PagingRqDto paging;

    @Override
    public String getKind() {
        return REFERENCE;
    }
}
