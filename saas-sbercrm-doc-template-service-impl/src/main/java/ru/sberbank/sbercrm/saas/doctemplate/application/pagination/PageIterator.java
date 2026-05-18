package ru.sberbank.sbercrm.saas.doctemplate.application.pagination;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Function;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import ru.sberbank.sbercrm.saas.doctemplate.shared.dto.CommonRqDto;

/**
 * Утилита для последовательного обхода постраничного API.
 *
 * <p>Инкапсулирует инкремент номера страницы и условия остановки, чтобы вызывающий код
 * работал с {@link Iterable} страниц и не дублировал paging-цикл в каждом gateway или обработчике.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PageIterator {

    /**
     * Возвращает lazy-итератор страниц, который вызывает {@code pageFetcher} только при переходе
     * к следующей странице.
     */
    public static <T> Iterable<List<T>> iteratePages(CommonRqDto request, Function<CommonRqDto, PageResult<T>> pageFetcher) {
        return () -> new Iterator<>() {
            private CommonRqDto nextRequest = request.toBuilder()
                .paging(request.getPaging().toBuilder().build())
                .build();
            private List<T> nextPage;
            private boolean prepared;
            private boolean finished;

            @Override
            public boolean hasNext() {
                prepareNextPage();
                return nextPage != null;
            }

            @Override
            public List<T> next() {
                prepareNextPage();
                if (nextPage == null) {
                    throw new NoSuchElementException("No more pages");
                }
                List<T> currentPage = nextPage;
                nextPage = null;
                prepared = false;
                return currentPage;
            }

            private void prepareNextPage() {
                if (prepared) {
                    return;
                }
                prepared = true;
                if (finished) {
                    nextPage = null;
                    return;
                }

                PageResult<T> page = pageFetcher.apply(nextRequest);
                nextPage = page.getData();
                if (nextPage.isEmpty()) {
                    finished = true;
                    nextPage = null;
                    return;
                }
                if (isLastPage(page, nextRequest)) {
                    finished = true;
                    return;
                }

                int nextPageNumber = nextRequest.getPaging().getPage() + 1;
                nextRequest = nextRequest.toBuilder()
                    .paging(nextRequest.getPaging().toBuilder().page(nextPageNumber).build())
                    .build();
            }
        };
    }

    private static <T> boolean isLastPage(PageResult<T> page, CommonRqDto request) {
        if (page.getPaging() != null) {
            if (page.getPaging().getTotalPageAmount() != null && page.getPaging().getTotalPageAmount() > 0) {
                long currentPage = page.getPaging().getCurrentPage() == null ? request.getPaging().getPage() : page.getPaging().getCurrentPage();
                if (currentPage + 1 >= page.getPaging().getTotalPageAmount()) {
                    return true;
                }
            }
            if (page.getPaging().getRecordsOnPage() != null && request.getPaging().getSize() != null) {
                return page.getPaging().getRecordsOnPage() < request.getPaging().getSize();
            }
        }
        return false;
    }
}
