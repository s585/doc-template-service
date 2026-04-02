package ru.sberbank.sbercrm.saas.doctemplate.document.converter;

import org.jooq.Record;
import org.jooq.RecordMapper;
import org.mapstruct.Mapper;
import ru.sberbank.sbercrm.jooq.tables.records.TGeneratedDocumentRecord;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.GeneratedDocument;

import static ru.sberbank.sbercrm.jooq.tables.TGeneratedDocument.T_GENERATED_DOCUMENT;

@Mapper(componentModel = "spring")
public interface GeneratedDocumentRecordConverter extends RecordMapper<Record, GeneratedDocument> {
    GeneratedDocument map(TGeneratedDocumentRecord record);

    @Override
    default GeneratedDocument map(Record record) {
        if (record == null) {
            return null;
        }
        return map(record.into(T_GENERATED_DOCUMENT));
    }
}
