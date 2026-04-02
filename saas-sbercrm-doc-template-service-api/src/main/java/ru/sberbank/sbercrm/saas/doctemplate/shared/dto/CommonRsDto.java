package ru.sberbank.sbercrm.saas.doctemplate.shared.dto;


import com.fasterxml.jackson.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.*;

@JsonPropertyOrder({"data", "paging", "additionalItems"})
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class CommonRsDto {

    @JsonProperty("data")
    private Object data;
    @JsonProperty("paging")
    private PagingRsDto paging;
    @JsonProperty("additionalItems")
    private Object additionalItems;

    @JsonIgnore
    @Builder.Default
    private final Map<String, Object> additionalProperties = new HashMap<>();
}
