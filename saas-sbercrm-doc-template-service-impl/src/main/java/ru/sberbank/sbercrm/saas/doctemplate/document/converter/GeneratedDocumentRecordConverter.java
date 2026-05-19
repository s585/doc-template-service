package ru.sberbank.sbercrm.saas.doctemplate.document.converter;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.jooq.Record;
import org.jooq.RecordMapper;
import org.mapstruct.Mapper;
import ru.sberbank.sbercrm.saas.doctemplate.jooq.tables.records.TGeneratedDocumentRecord;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.GeneratedDocument;

import static ru.sberbank.sbercrm.saas.doctemplate.jooq.tables.TGeneratedDocument.T_GENERATED_DOCUMENT;

@Mapper(componentModel = "spring")
public interface GeneratedDocumentRecordConverter extends RecordMapper<Record, GeneratedDocument> {
    GeneratedDocument map(TGeneratedDocumentRecord record);

    @Override
    @Nullable
    default GeneratedDocument map(@Nullable Record record) {
        if (record == null) {
            return null;
        }
        return map(record.into(T_GENERATED_DOCUMENT));
    }
}
