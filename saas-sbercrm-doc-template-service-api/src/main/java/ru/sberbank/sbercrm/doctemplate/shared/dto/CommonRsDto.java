package ru.sberbank.sbercrm.doctemplate.shared.dto;


import com.fasterxml.jackson.annotation.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.*;

@JsonPropertyOrder({"data", "paging", "additionalItems"})
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class CommonRsDto implements Serializable {
    private static final long serialVersionUID = 7490832299763596282L;

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
