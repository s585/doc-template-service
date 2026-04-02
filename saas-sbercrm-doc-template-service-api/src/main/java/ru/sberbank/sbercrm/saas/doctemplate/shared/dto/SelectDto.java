package ru.sberbank.sbercrm.saas.doctemplate.shared.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.Set;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Schema(title = "Данные для получения в ответ")
public class SelectDto {
    public static final SelectDto EMPTY = SelectDto.builder().fields(Collections.emptySet()).build();
    @Schema(title = "Список полей для получения в ответ")
    private Set<String> fields;
}
