package ru.sberbank.sbercrm.saas.doctemplate.application.pagination;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.sberbank.sbercrm.saas.doctemplate.shared.dto.CommonRqDto;
import ru.sberbank.sbercrm.saas.doctemplate.shared.dto.PagingRqDto;
import ru.sberbank.sbercrm.saas.doctemplate.shared.dto.PagingRsDto;

class PageIteratorTest {

    @Test
    @DisplayName("Итератор обходит все страницы через переданный fetcher")
    void givenMultiPageResponse_whenIteratePages_thenYieldEveryPage() {
        CommonRqDto request = CommonRqDto.builder().paging(PagingRqDto.builder().page(0).size(2).build()).build();

        List<List<Map<String, Object>>> pages = new ArrayList<>();
        for (List<Map<String, Object>> page : PageIterator.iteratePages(
            request,
            pageRequest -> switch (pageRequest.getPaging().getPage()) {
                case 0 -> PageResult.<Map<String, Object>>builder()
                    .data(List.of(
                        Map.of("id", "1", "name", "Product A"),
                        Map.of("id", "2", "name", "Product B")
                    ))
                    .paging(PagingRsDto.builder().currentPage(0L).totalPageAmount(2L).recordsOnPage(2L).build())
                    .build();
                case 1 -> PageResult.<Map<String, Object>>builder()
                    .data(List.of(Map.of("id", "3", "name", "Product C")))
                    .paging(PagingRsDto.builder().currentPage(1L).totalPageAmount(2L).recordsOnPage(1L).build())
                    .build();
                default -> throw new IllegalStateException("Unexpected page request");
            }
        )) {
            pages.add(page);
        }

        assertThat(pages).hasSize(2);
        assertThat(pages.getFirst()).hasSize(2);
        assertThat(pages.get(1))
            .extracting(item -> item.get("name"))
            .containsExactly("Product C");
    }
}
