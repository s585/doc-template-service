package ru.sberbank.sbercrm.doctemplate.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Collections;
import java.util.Set;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Schema(title = "Данные для получения в ответ")
public class SelectDto implements Serializable {
    public static final SelectDto EMPTY = SelectDto.builder().fields(Collections.emptySet()).build();
    private static final long serialVersionUID = 7533824739870233802L;
    @Schema(title = "Список полей для получения в ответ")
    private Set<String> fields;
}
