package ru.sberbank.sbercrm.doctemplate.template.converter;

import lombok.RequiredArgsConstructor;
import org.jooq.Record;
import org.springframework.stereotype.Component;
import ru.sberbank.sbercrm.doctemplate.common.helper.JsonbHelper;
import ru.sberbank.sbercrm.doctemplate.template.model.Template;
import ru.sberbank.sbercrm.doctemplate.template.model.TemplateFormat;
import ru.sberbank.sbercrm.doctemplate.template.model.rule.Rule;

import static ru.sberbank.sbercrm.jooq.tables.TTemplate.T_TEMPLATE;

@Component
@RequiredArgsConstructor
public class TemplateRecordConverter {
    private final JsonbHelper jsonbHelper;

    public Template convert(Record record) {
        if (record == null) {
            return null;
        }

        return Template.builder()
            .id(record.get(T_TEMPLATE.ID))
            .entityId(record.get(T_TEMPLATE.ENTITY_ID))
            .name(record.get(T_TEMPLATE.NAME))
            .code(record.get(T_TEMPLATE.CODE))
            .description(record.get(T_TEMPLATE.DESCRIPTION))
            .format(TemplateFormat.fromValue(record.get(T_TEMPLATE.FORMAT)))
            .s3Key(record.get(T_TEMPLATE.S3_KEY))
            .active(Boolean.TRUE.equals(record.get(T_TEMPLATE.ACTIVE)))
            .displayCondition(jsonbHelper.fromJsonb(
                record.get(T_TEMPLATE.DISPLAY_CONDITION),
                Rule.class
            ))
            .createdBy(record.get(T_TEMPLATE.CREATED_BY))
            .updatedBy(record.get(T_TEMPLATE.UPDATED_BY))
            .createdAt(record.get(T_TEMPLATE.CREATED_AT))
            .updatedAt(record.get(T_TEMPLATE.UPDATED_AT))
            .build();
    }
}
