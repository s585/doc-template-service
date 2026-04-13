package ru.sberbank.sbercrm.saas.doctemplate.document.converter;

import org.jooq.Record;
import org.jooq.RecordMapper;
import org.mapstruct.Mapper;
import ru.sberbank.sbercrm.saas.doctemplate.jooq.tables.records.TGenerationJobAttemptRecord;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.GenerationJobAttempt;

import static ru.sberbank.sbercrm.saas.doctemplate.jooq.tables.TGenerationJobAttempt.T_GENERATION_JOB_ATTEMPT;

@Mapper(componentModel = "spring")
public interface GenerationJobAttemptRecordConverter extends RecordMapper<Record, GenerationJobAttempt> {
    GenerationJobAttempt map(TGenerationJobAttemptRecord record);

    @Override
    default GenerationJobAttempt map(Record record) {
        if (record == null) {
            return null;
        }
        return map(record.into(T_GENERATION_JOB_ATTEMPT));
    }
}
