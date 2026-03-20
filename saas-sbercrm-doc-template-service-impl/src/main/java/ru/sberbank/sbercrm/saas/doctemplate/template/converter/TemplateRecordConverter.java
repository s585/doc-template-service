package ru.sberbank.sbercrm.saas.doctemplate.template.converter;

import org.jooq.Record;
import org.jooq.RecordMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import ru.sberbank.sbercrm.jooq.tables.records.TTemplateRecord;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.Template;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.TemplateFormat;

import static ru.sberbank.sbercrm.jooq.tables.TTemplate.T_TEMPLATE;

@Mapper(componentModel = "spring", uses = TemplateRecordAfterConverter.class)
public interface TemplateRecordConverter extends RecordMapper<Record, Template> {
    @Mapping(target = "format", source = "format", qualifiedByName = "convertFormat")
    @Mapping(target = "active", source = "active", qualifiedByName = "convertActive")
    @Mapping(target = "displayCondition", ignore = true)
    @Mapping(target = "mappings", ignore = true)
    Template map(TTemplateRecord record);

    @Override
    default Template map(Record record) {
        if (record == null) {
            return null;
        }
        return map(record.into(T_TEMPLATE));
    }

    @Named("convertFormat")
    default TemplateFormat convertFormat(String value) {
        return value == null ? null : TemplateFormat.fromValue(value);
    }

    @Named("convertActive")
    default boolean convertActive(Boolean value) {
        return Boolean.TRUE.equals(value);
    }
}
