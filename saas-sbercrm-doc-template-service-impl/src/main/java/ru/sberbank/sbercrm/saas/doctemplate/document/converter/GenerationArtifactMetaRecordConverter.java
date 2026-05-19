package ru.sberbank.sbercrm.saas.doctemplate.document.converter;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.jooq.Record3;
import org.jooq.RecordMapper;
import org.mapstruct.Mapper;
import ru.sberbank.sbercrm.saas.doctemplate.document.model.GenerationArtifactMeta;

@Mapper(componentModel = "spring")
public interface GenerationArtifactMetaRecordConverter extends RecordMapper<Record3<String, String, Long>, GenerationArtifactMeta> {
    @Override
    @Nullable
    default GenerationArtifactMeta map(@Nullable Record3<String, String, Long> record) {
        if (record == null) {
            return null;
        }
        return GenerationArtifactMeta.builder()
            .s3Key(record.value1())
            .checksum(record.value2())
            .sizeBytes(record.value3())
            .build();
    }
}
