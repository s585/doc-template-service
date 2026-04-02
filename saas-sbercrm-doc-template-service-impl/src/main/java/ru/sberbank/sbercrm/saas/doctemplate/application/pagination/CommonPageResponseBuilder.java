package ru.sberbank.sbercrm.saas.doctemplate.application.pagination;

import java.util.function.Function;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import ru.sberbank.sbercrm.saas.doctemplate.shared.dto.CommonRqDto;
import ru.sberbank.sbercrm.saas.doctemplate.shared.dto.CommonRsDto;
import ru.sberbank.sbercrm.saas.doctemplate.shared.dto.PagingRsDto;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CommonPageResponseBuilder {

    public static <T, R> CommonRsDto build(
        CommonRqDto request,
        PageResult<T> result,
        Function<T, R> mapper
    ) {
        long pageSize = request.getPaging().getSize();

        return CommonRsDto.builder()
            .data(result.getData().stream().map(mapper).toList())
            .paging(
                PagingRsDto.builder()
                    .currentPage(request.getPaging().getPage().longValue())
                    .recordsOnPage((long) result.getData().size())
                    .totalRecordsAmount(result.getTotalRecordsAmount())
                    .totalPageAmount(pageSize == 0 ? 0L : (result.getTotalRecordsAmount() + pageSize - 1) / pageSize)
                    .build()
            )
            .build();
    }
}
