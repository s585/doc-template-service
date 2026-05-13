package ru.sberbank.sbercrm.saas.doctemplate.application.integration.gateway;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.sberbank.sbercrm.saas.doctemplate.shared.dto.CommonRqDto;
import ru.sberbank.sbercrm.saas.doctemplate.shared.dto.PagingRqDto;
import ru.sberbank.sbercrm.saas.doctemplate.shared.dto.PagingRsDto;

@ExtendWith(MockitoExtension.class)
class BusinessObjectPageIteratorTest {
    private static final UUID TENANT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID ENTITY_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @Mock
    private BusinessObjectGateway businessObjectGateway;

    @Test
    @DisplayName("Итератор обходит все страницы list-objects")
    void givenMultiPageResponse_whenIterateListObjectPages_thenYieldEveryPage() {
        BusinessObjectPageIterator systemUnderTest = new BusinessObjectPageIterator(businessObjectGateway);
        CommonRqDto request = CommonRqDto.builder().paging(PagingRqDto.builder().page(0).size(2).build()).build();
        PagedResult<Map<String, Object>> firstPage = new PagedResult<>(
            List.of(
                Map.of("id", "1", "name", "Product A"),
                Map.of("id", "2", "name", "Product B")
            ),
            PagingRsDto.builder().currentPage(0L).totalPageAmount(2L).recordsOnPage(2L).build()
        );
        PagedResult<Map<String, Object>> secondPage = new PagedResult<>(
            List.of(
                Map.of("id", "3", "name", "Product C")
            ),
            PagingRsDto.builder().currentPage(1L).totalPageAmount(2L).recordsOnPage(1L).build()
        );
        given(businessObjectGateway.getListObjectsPage(TENANT_ID, USER_ID, ENTITY_ID, request)).willReturn(firstPage);
        given(businessObjectGateway.getListObjectsPage(
            TENANT_ID,
            USER_ID,
            ENTITY_ID,
            request.toBuilder().paging(request.getPaging().toBuilder().page(1).build()).build()
        )).willReturn(secondPage);

        List<List<Map<String, Object>>> pages = new ArrayList<>();
        for (List<Map<String, Object>> page : systemUnderTest.iterateListObjectPages(TENANT_ID, USER_ID, ENTITY_ID, request)) {
            pages.add(page);
        }

        assertThat(pages).hasSize(2);
        assertThat(pages.getFirst()).hasSize(2);
        assertThat(pages.get(1))
            .extracting(item -> item.get("name"))
            .containsExactly("Product C");
    }
}
