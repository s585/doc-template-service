package ru.sberbank.sbercrm.saas.doctemplate.application.integration.gateway;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.sberbank.sbercrm.saas.doctemplate.shared.dto.CommonRqDto;

@Component
@RequiredArgsConstructor
public class BusinessObjectPageIterator {
    private final BusinessObjectGateway businessObjectGateway;

    public Iterable<List<Map<String, Object>>> iterateListObjectPages(UUID tenantId, UUID userId, UUID entityId, CommonRqDto request) {
        return () -> new Iterator<>() {
            private CommonRqDto nextRequest = request.toBuilder()
                .paging(request.getPaging().toBuilder().build())
                .build();
            private List<Map<String, Object>> nextPage;
            private boolean prepared;
            private boolean finished;

            @Override
            public boolean hasNext() {
                prepareNextPage();
                return nextPage != null;
            }

            @Override
            public List<Map<String, Object>> next() {
                prepareNextPage();
                if (nextPage == null) {
                    throw new NoSuchElementException("No more business object pages");
                }
                List<Map<String, Object>> currentPage = nextPage;
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

                PagedResult<Map<String, Object>> page = businessObjectGateway.getListObjectsPage(
                    tenantId,
                    userId,
                    entityId,
                    nextRequest
                );
                nextPage = page.items();
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

    private boolean isLastPage(PagedResult<Map<String, Object>> page, CommonRqDto request) {
        if (page.paging() != null) {
            if (page.paging().getTotalPageAmount() != null && page.paging().getTotalPageAmount() > 0) {
                long currentPage = page.paging().getCurrentPage() == null ? request.getPaging().getPage() : page.paging().getCurrentPage();
                if (currentPage + 1 >= page.paging().getTotalPageAmount()) {
                    return true;
                }
            }
            if (page.paging().getRecordsOnPage() != null && request.getPaging().getSize() != null) {
                return page.paging().getRecordsOnPage() < request.getPaging().getSize();
            }
        }
        return false;
    }
}
