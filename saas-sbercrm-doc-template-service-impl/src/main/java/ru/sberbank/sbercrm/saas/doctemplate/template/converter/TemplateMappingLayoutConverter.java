package ru.sberbank.sbercrm.saas.doctemplate.template.converter;

import org.mapstruct.Mapper;
import org.mapstruct.Named;
import ru.sberbank.sbercrm.saas.doctemplate.template.dto.TemplateMappingLayoutDto;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.TemplateMappingLayout;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.source.ValueSourceKind;

@Mapper(componentModel = "spring")
public interface TemplateMappingLayoutConverter {
    TemplateMappingLayoutDto convertToDto(TemplateMappingLayout model);

    @Named("convertToKindValue")
    default String convertToKindValue(ValueSourceKind value) {
        return value.value();
    }
}
