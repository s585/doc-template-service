package ru.sberbank.sbercrm.doctemplate.template.converter;

import lombok.RequiredArgsConstructor;
import org.jooq.Record;
import org.springframework.stereotype.Component;
import ru.sberbank.sbercrm.doctemplate.common.helper.JsonbHelper;
import ru.sberbank.sbercrm.doctemplate.template.model.TemplateMapping;
import ru.sberbank.sbercrm.doctemplate.template.model.TemplateMappingDefinition;

import static ru.sberbank.sbercrm.jooq.tables.TTemplateMapping.T_TEMPLATE_MAPPING;

@Component
@RequiredArgsConstructor
public class TemplateMappingRecordConverter {
    private final JsonbHelper jsonbHelper;

    public TemplateMapping convert(Record record) {
        if (record == null) {
            return null;
        }

        return TemplateMapping.builder()
            .id(record.get(T_TEMPLATE_MAPPING.ID))
            .key(record.get(T_TEMPLATE_MAPPING.KEY))
            .definition(jsonbHelper.fromJsonb(
                record.get(T_TEMPLATE_MAPPING.DEFINITION),
                TemplateMappingDefinition.class
            ))
            .build();
    }
}
