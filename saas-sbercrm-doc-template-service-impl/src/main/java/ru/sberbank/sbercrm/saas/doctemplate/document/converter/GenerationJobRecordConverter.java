package ru.sberbank.sbercrm.saas.doctemplate.document.converter;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.jooq.Record;
import org.jooq.RecordMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.sberbank.sbercrm.saas.doctemplate.jooq.tables.records.TGenerationJobRecord;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.GenerationJob;

import static ru.sberbank.sbercrm.saas.doctemplate.jooq.tables.TGenerationJob.T_GENERATION_JOB;

@Mapper(componentModel = "spring")
public interface GenerationJobRecordConverter extends RecordMapper<Record, GenerationJob> {
    @Mapping(target = "tenantId", source = "tenantId")
    @Mapping(target = "createdBy", source = "createdBy")
    @Mapping(target = "updatedBy", source = "updatedBy")
    GenerationJob map(TGenerationJobRecord record);

    @Override
    @Nullable
    default GenerationJob map(@Nullable Record record) {
        if (record == null) {
            return null;
        }
        return map(record.into(T_GENERATION_JOB));
    }
}
