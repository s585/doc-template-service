package ru.sberbank.sbercrm.saas.doctemplate.application.integration.gateway;

import java.util.List;
import ru.sberbank.sbercrm.saas.doctemplate.shared.dto.PagingRsDto;

public record PagedResult<T>(
    List<T> items,
    PagingRsDto paging
) {
}
