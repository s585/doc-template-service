package ru.sberbank.sbercrm.saas.doctemplate.template.converter;

import lombok.RequiredArgsConstructor;
import org.mapstruct.AfterMapping;
import org.mapstruct.MappingTarget;
import org.springframework.stereotype.Component;
import ru.sberbank.sbercrm.saas.doctemplate.jooq.tables.records.TTemplateMappingRecord;
import ru.sberbank.sbercrm.saas.doctemplate.application.jooq.JsonbHelper;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.TemplateMapping;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.TemplateMappingDefinition;

@Component
@RequiredArgsConstructor
public class TemplateMappingRecordAfterConverter {
    private final JsonbHelper jsonbHelper;

    @AfterMapping
    public void populateJsonbFields(
        TTemplateMappingRecord record,
        @MappingTarget TemplateMapping.TemplateMappingBuilder templateMapping
    ) {
        templateMapping.definition(
            jsonbHelper.fromJsonb(record.getDefinition(), TemplateMappingDefinition.class)
        );
    }
}
