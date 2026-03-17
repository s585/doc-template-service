package ru.sberbank.sbercrm.doctemplate.common;


import com.fasterxml.jackson.annotation.*;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;
import java.util.*;

@JsonPropertyOrder({"currentPage", "totalPageAmount", "recordsOnPage", "totalRecordsAmount"})
@Getter
@EqualsAndHashCode
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class PagingRsDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 4140780685045497706L;
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
