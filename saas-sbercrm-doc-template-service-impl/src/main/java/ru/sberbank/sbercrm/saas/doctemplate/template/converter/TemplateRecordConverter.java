package ru.sberbank.sbercrm.saas.doctemplate.template.converter;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.jooq.Record;
import org.jooq.RecordMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import ru.sberbank.sbercrm.saas.doctemplate.jooq.tables.records.TTemplateRecord;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.Template;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.TemplateFormat;

import static ru.sberbank.sbercrm.saas.doctemplate.jooq.tables.TTemplate.T_TEMPLATE;

@Mapper(componentModel = "spring", uses = TemplateRecordAfterConverter.class)
public interface TemplateRecordConverter extends RecordMapper<Record, Template> {
    @Mapping(target = "format", source = "format", qualifiedByName = "convertFormat")
    @Mapping(target = "active", source = "active", qualifiedByName = "convertActive")
    @Mapping(target = "displayCondition", ignore = true)
    @Mapping(target = "mappings", ignore = true)
    Template map(TTemplateRecord record);

    @Override
    @Nullable
    default Template map(@Nullable Record record) {
        if (record == null) {
            return null;
        }
        return map(record.into(T_TEMPLATE));
    }

    @Named("convertFormat")
    default TemplateFormat convertFormat(String value) {
        return TemplateFormat.fromValue(value);
    }

    @Named("convertActive")
    default boolean convertActive(Boolean value) {
        return value;
    }
}
