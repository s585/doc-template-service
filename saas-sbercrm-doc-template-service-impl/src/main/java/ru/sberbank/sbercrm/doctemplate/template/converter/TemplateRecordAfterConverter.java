package ru.sberbank.sbercrm.doctemplate.template.converter;

import lombok.RequiredArgsConstructor;
import org.mapstruct.AfterMapping;
import org.mapstruct.MappingTarget;
import org.springframework.stereotype.Component;
import ru.sberbank.sbercrm.jooq.tables.records.TTemplateRecord;
import ru.sberbank.sbercrm.doctemplate.common.helper.JsonbHelper;
import ru.sberbank.sbercrm.doctemplate.template.model.Template;
import ru.sberbank.sbercrm.doctemplate.template.model.rule.Rule;

@Component
@RequiredArgsConstructor
public class TemplateRecordAfterConverter {
    private final JsonbHelper jsonbHelper;

    @AfterMapping
    public void populateJsonbFields(TTemplateRecord record, @MappingTarget Template.TemplateBuilder template) {
        template.displayCondition(jsonbHelper.fromJsonb(record.getDisplayCondition(), Rule.class));
    }
}
