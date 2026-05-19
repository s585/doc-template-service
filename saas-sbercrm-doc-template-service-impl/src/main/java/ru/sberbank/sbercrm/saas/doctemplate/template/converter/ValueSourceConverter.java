package ru.sberbank.sbercrm.saas.doctemplate.template.converter;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.mapstruct.Mapper;
import ru.sberbank.sbercrm.saas.doctemplate.template.dto.source.ConstantValueSourceDto;
import ru.sberbank.sbercrm.saas.doctemplate.template.dto.source.DirectValueSourceDto;
import ru.sberbank.sbercrm.saas.doctemplate.template.dto.source.ReferenceValueSourceDto;
import ru.sberbank.sbercrm.saas.doctemplate.template.dto.source.ValueSourceDto;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.source.ConstantValueSource;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.source.DirectValueSource;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.source.ReferenceValueSource;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.source.ValueSource;

@Mapper(componentModel = "spring")
public interface ValueSourceConverter {
    @Nullable
    default ValueSource convertToModel(@Nullable ValueSourceDto dto) {
        if (dto == null) {
            return null;
        }
        return switch (dto) {
            case DirectValueSourceDto directValueSourceDto -> convertDirectValueSourceToModel(directValueSourceDto);
            case ReferenceValueSourceDto referenceValueSourceDto ->
                convertReferenceValueSourceToModel(referenceValueSourceDto);
            case ConstantValueSourceDto constantValueSourceDto ->
                convertConstantValueSourceToModel(constantValueSourceDto);
            default -> throw new IllegalArgumentException("Unsupported value source dto type: " + dto.getClass().getName());
        };
    }

    @Nullable
    default ValueSourceDto convertToDto(@Nullable ValueSource model) {
        return switch (model) {
            case null -> null;
            case DirectValueSource directValueSource -> convertDirectValueSourceToDto(directValueSource);
            case ReferenceValueSource referenceValueSource -> convertReferenceValueSourceToDto(referenceValueSource);
            case ConstantValueSource constantValueSource -> convertConstantValueSourceToDto(constantValueSource);
        };
    }

    DirectValueSource convertDirectValueSourceToModel(DirectValueSourceDto dto);

    ReferenceValueSource convertReferenceValueSourceToModel(ReferenceValueSourceDto dto);

    ConstantValueSource convertConstantValueSourceToModel(ConstantValueSourceDto dto);

    DirectValueSourceDto convertDirectValueSourceToDto(DirectValueSource model);

    ReferenceValueSourceDto convertReferenceValueSourceToDto(ReferenceValueSource model);

    ConstantValueSourceDto convertConstantValueSourceToDto(ConstantValueSource model);
}
