package ru.sberbank.sbercrm.saas.doctemplate.template.converter;

import org.mapstruct.Mapper;
import ru.sberbank.sbercrm.doctemplate.template.dto.source.ConstantValueSourceDto;
import ru.sberbank.sbercrm.doctemplate.template.dto.source.DirectValueSourceDto;
import ru.sberbank.sbercrm.doctemplate.template.dto.source.ReferenceValueSourceDto;
import ru.sberbank.sbercrm.doctemplate.template.dto.source.ValueSourceDto;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.source.ConstantValueSource;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.source.DirectValueSource;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.source.ReferenceValueSource;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.source.ValueSource;

@Mapper(componentModel = "spring")
public interface ValueSourceConverter {
    default ValueSource convertToModel(ValueSourceDto dto) {
        return switch (dto) {
            case null -> null;
            case DirectValueSourceDto directValueSourceDto -> convertDirectValueSourceToModel(directValueSourceDto);
            case ReferenceValueSourceDto referenceValueSourceDto ->
                convertReferenceValueSourceToModel(referenceValueSourceDto);
            case ConstantValueSourceDto constantValueSourceDto ->
                convertConstantValueSourceToModel(constantValueSourceDto);
            default -> throw new IllegalArgumentException("Unsupported value source dto type: " + dto.getClass().getName());
        };
    }

    default ValueSourceDto convertToDto(ValueSource model) {
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
