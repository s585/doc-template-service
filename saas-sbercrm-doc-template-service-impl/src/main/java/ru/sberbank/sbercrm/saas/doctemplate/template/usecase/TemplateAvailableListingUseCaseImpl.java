package ru.sberbank.sbercrm.saas.doctemplate.template.usecase;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;
import ru.sberbank.sbercrm.saas.doctemplate.application.integration.gateway.BusinessObjectGateway;
import ru.sberbank.sbercrm.saas.doctemplate.shared.dto.CheckDataByEachFilterRsDto;
import ru.sberbank.sbercrm.saas.doctemplate.shared.dto.FilterDto;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.Template;
import ru.sberbank.sbercrm.saas.doctemplate.template.service.TemplateService;

@Component
@RequiredArgsConstructor
public class TemplateAvailableListingUseCaseImpl implements TemplateAvailableListingUseCase {
    private final TemplateService templateService;
    private final BusinessObjectGateway businessObjectGateway;

    @Override
    public List<Template> execute(UUID tenantId, UUID userId, UUID entityId, UUID objectId) {
        Map<String, Object> businessObject = businessObjectGateway.getObject(tenantId, userId, entityId, objectId);
        return filterAvailableTemplates(
            tenantId,
            userId,
            entityId,
            businessObject,
            templateService.findAllActiveByEntityIdOrderByNameAndId(tenantId, entityId)
        );
    }

    private List<Template> filterAvailableTemplates(
        UUID tenantId,
        UUID userId,
        UUID entityId,
        Map<String, Object> businessObject,
        List<Template> templates
    ) {
        List<Template> templatesWithCondition = templates.stream()
            .filter(template -> template.getDisplayCondition() != null)
            .toList();
        List<FilterDto> filters = templatesWithCondition.stream()
            .map(Template::getDisplayCondition)
            .toList();
        if (CollectionUtils.isEmpty(filters)) {
            return templates;
        }

        List<CheckDataByEachFilterRsDto> checkResults = businessObjectGateway.checkDataByEachFilter(
            tenantId,
            userId,
            entityId,
            businessObject,
            filters
        );

        List<Template> result = new ArrayList<>(templates.size());
        int conditionalIndex = 0;
        for (Template template : templates) {
            if (template.getDisplayCondition() == null) {
                result.add(template);
                continue;
            }
            if (conditionalIndex < checkResults.size()
                && Boolean.TRUE.equals(checkResults.get(conditionalIndex).getResult())) {
                result.add(template);
            }
            conditionalIndex++;
        }
        return result;
    }
}
