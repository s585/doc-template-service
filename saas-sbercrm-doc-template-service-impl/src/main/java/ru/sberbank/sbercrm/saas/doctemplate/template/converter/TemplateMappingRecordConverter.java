package ru.sberbank.sbercrm.saas.doctemplate.template.converter;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.jooq.Record;
import org.jooq.RecordMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.sberbank.sbercrm.saas.doctemplate.jooq.tables.records.TTemplateMappingRecord;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.TemplateMapping;

import static ru.sberbank.sbercrm.saas.doctemplate.jooq.tables.TTemplateMapping.T_TEMPLATE_MAPPING;

@Mapper(componentModel = "spring", uses = TemplateMappingRecordAfterConverter.class)
public interface TemplateMappingRecordConverter extends RecordMapper<Record, TemplateMapping> {
    @Mapping(target = "definition", ignore = true)
    TemplateMapping map(TTemplateMappingRecord record);

    @Override
    @Nullable
    default TemplateMapping map(@Nullable Record record) {
        if (record == null) {
            return null;
        }
        return map(record.into(T_TEMPLATE_MAPPING));
    }
}
