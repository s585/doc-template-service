package ru.sberbank.sbercrm.saas.doctemplate.template.converter;

import lombok.RequiredArgsConstructor;
import org.mapstruct.AfterMapping;
import org.mapstruct.MappingTarget;
import org.springframework.stereotype.Component;
import ru.sberbank.sbercrm.jooq.tables.records.TTemplateRecord;
import ru.sberbank.sbercrm.saas.doctemplate.shared.dto.FilterDto;
import ru.sberbank.sbercrm.saas.doctemplate.application.jooq.JsonbHelper;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.Template;

@Component
@RequiredArgsConstructor
public class TemplateRecordAfterConverter {
    private final JsonbHelper jsonbHelper;

    @AfterMapping
    public void populateJsonbFields(TTemplateRecord record, @MappingTarget Template.TemplateBuilder template) {
        template.displayCondition(jsonbHelper.fromJsonb(record.getDisplayCondition(), FilterDto.class));
    }
}
