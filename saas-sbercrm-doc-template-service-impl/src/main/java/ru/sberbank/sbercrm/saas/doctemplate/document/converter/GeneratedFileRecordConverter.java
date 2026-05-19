package ru.sberbank.sbercrm.saas.doctemplate.document.converter;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.jooq.Record;
import org.jooq.RecordMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.sberbank.sbercrm.saas.doctemplate.jooq.tables.records.TGeneratedFileRecord;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.GeneratedFile;

import static ru.sberbank.sbercrm.saas.doctemplate.jooq.tables.TGeneratedFile.T_GENERATED_FILE;

@Mapper(componentModel = "spring")
public interface GeneratedFileRecordConverter extends RecordMapper<Record, GeneratedFile> {
    @Mapping(target = "createdBy", source = "createdBy")
    @Mapping(target = "updatedBy", source = "updatedBy")
    GeneratedFile map(TGeneratedFileRecord record);

    @Override
    @Nullable
    default GeneratedFile map(@Nullable Record record) {
        if (record == null) {
            return null;
        }
        return map(record.into(T_GENERATED_FILE));
    }
}
