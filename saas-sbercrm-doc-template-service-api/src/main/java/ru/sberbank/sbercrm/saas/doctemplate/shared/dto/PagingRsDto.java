package ru.sberbank.sbercrm.saas.doctemplate.shared.dto;


import com.fasterxml.jackson.annotation.*;
import lombok.*;

import java.util.*;

@JsonPropertyOrder({"currentPage", "totalPageAmount", "recordsOnPage", "totalRecordsAmount"})
@Getter
@EqualsAndHashCode
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class PagingRsDto {
    @JsonIgnore
    @Builder.Default
    private final Map<String, Object> additionalProperties = new HashMap<>();
    @JsonProperty("currentPage")
    private Long currentPage;
    @JsonProperty("totalPageAmount")
    private Long totalPageAmount;
    @JsonProperty("recordsOnPage")
    private Long recordsOnPage;
    @JsonProperty("totalRecordsAmount")
    private Long totalRecordsAmount;
}
