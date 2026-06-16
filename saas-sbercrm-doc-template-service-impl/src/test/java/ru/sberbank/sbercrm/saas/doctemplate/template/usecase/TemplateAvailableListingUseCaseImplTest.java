package ru.sberbank.sbercrm.saas.doctemplate.template.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.sberbank.sbercrm.saas.doctemplate.application.integration.gateway.BusinessObjectGateway;
import ru.sberbank.sbercrm.saas.doctemplate.shared.dto.CheckDataByFilterRsDto;
import ru.sberbank.sbercrm.saas.doctemplate.shared.dto.FilterDto;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.Template;
import ru.sberbank.sbercrm.saas.doctemplate.template.service.TemplateService;

@ExtendWith(MockitoExtension.class)
class TemplateAvailableListingUseCaseImplTest {
    private static final UUID TENANT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID ENTITY_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID OBJECT_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");

    @Mock
    private TemplateService templateService;

    @Mock
    private BusinessObjectGateway businessObjectGateway;

    @InjectMocks
    private TemplateAvailableListingUseCaseImpl systemUnderTest;

    @Test
    @DisplayName("Доступные шаблоны включают шаблоны без условий и шаблоны с true результатом проверки")
    void givenTemplatesWithConditions_whenExecute_thenFilterByCoreCheckResultsInRequestOrder() {
        Map<String, Object> businessObject = Map.of("source", Map.of("status", "APPROVED"));
        FilterDto firstCondition = condition("source.status", "APPROVED");
        FilterDto secondCondition = condition("source.amount", 100);
        FilterDto thirdCondition = condition("source.type", "CONTRACT");
        Template withoutCondition = template("without-condition", null);
        Template firstAvailable = template("first-available", firstCondition);
        Template rejected = template("rejected", secondCondition);
        Template secondAvailable = template("second-available", thirdCondition);

        given(businessObjectGateway.getObject(TENANT_ID, USER_ID, ENTITY_ID, OBJECT_ID)).willReturn(businessObject);
        given(templateService.findAllActiveByEntityIdOrderByNameAndId(TENANT_ID, ENTITY_ID))
            .willReturn(List.of(withoutCondition, firstAvailable, rejected, secondAvailable));
        given(businessObjectGateway.checkDataByEachFilter(
            TENANT_ID,
            USER_ID,
            ENTITY_ID,
            businessObject,
            List.of(firstCondition, secondCondition, thirdCondition)
        )).willReturn(List.of(
            checkResult(true),
            checkResult(false),
            checkResult(true)
        ));

        List<Template> result = systemUnderTest.execute(TENANT_ID, USER_ID, ENTITY_ID, OBJECT_ID);

        assertThat(result).containsExactly(withoutCondition, firstAvailable, secondAvailable);
    }

    @Test
    @DisplayName("Шаблоны без displayCondition не отправляются на проверку условий")
    void givenTemplatesWithoutConditions_whenExecute_thenSkipCoreChecks() {
        Map<String, Object> businessObject = Map.of("source", Map.of("status", "APPROVED"));
        Template first = template("first", null);
        Template second = template("second", null);

        given(businessObjectGateway.getObject(TENANT_ID, USER_ID, ENTITY_ID, OBJECT_ID)).willReturn(businessObject);
        given(templateService.findAllActiveByEntityIdOrderByNameAndId(TENANT_ID, ENTITY_ID)).willReturn(List.of(first, second));

        List<Template> result = systemUnderTest.execute(TENANT_ID, USER_ID, ENTITY_ID, OBJECT_ID);

        assertThat(result).containsExactly(first, second);
        verify(businessObjectGateway, never()).checkDataByEachFilter(any(), any(), any(), any(), any());
    }

    private Template template(String code, FilterDto displayCondition) {
        return Template.builder()
            .id(UUID.nameUUIDFromBytes(code.getBytes(java.nio.charset.StandardCharsets.UTF_8)))
            .code(code)
            .displayCondition(displayCondition)
            .active(true)
            .build();
    }

    private FilterDto condition(String field, Object value) {
        return FilterDto.builder()
            .field(field)
            .operation(FilterDto.Operation.EQUAL)
            .value(List.of(value))
            .build();
    }

    private CheckDataByFilterRsDto checkResult(boolean result) {
        return CheckDataByFilterRsDto.builder()
            .result(result)
            .build();
    }
}
