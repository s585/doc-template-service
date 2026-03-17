package ru.sberbank.sbercrm.doctemplate.common;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder({"page", "size"})
@Schema(description = "Параметры пагинации запроса")
public class PagingRqDto implements Serializable {
    private static final long serialVersionUID = 7867444848483912495L;

    @NotNull
    @Schema(description = "Номер страницы, начиная с 0")
    private Integer page;

    @NotNull
    @Schema(description = "Размер страницы")
    private Integer size;
}
