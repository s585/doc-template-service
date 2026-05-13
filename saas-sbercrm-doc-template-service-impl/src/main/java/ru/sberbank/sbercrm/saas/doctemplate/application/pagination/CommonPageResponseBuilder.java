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
        return CommonRsDto.builder()
            .data(result.getData().stream().map(mapper).toList())
            .paging(result.getPaging() == null ? buildPaging(request, result.getData().size(), 0L) : result.getPaging())
            .build();
    }

    public static PagingRsDto buildPaging(CommonRqDto request, int recordsOnPage, long totalRecordsAmount) {
        long pageSize = request.getPaging().getSize();
        return PagingRsDto.builder()
            .currentPage(request.getPaging().getPage().longValue())
            .recordsOnPage((long) recordsOnPage)
            .totalRecordsAmount(totalRecordsAmount)
            .totalPageAmount(pageSize == 0 ? 0L : (totalRecordsAmount + pageSize - 1) / pageSize)
            .build();
    }
}
