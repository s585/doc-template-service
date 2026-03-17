package ru.sberbank.sbercrm.doctemplate.template.converter;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import ru.sberbank.sbercrm.doctemplate.template.TemplateMappingDefinitionDto;
import ru.sberbank.sbercrm.doctemplate.template.model.MappingScope;
import ru.sberbank.sbercrm.doctemplate.template.model.TemplateMappingDefinition;
import ru.sberbank.sbercrm.doctemplate.template.model.TemplateValueType;

@Mapper(componentModel = "spring", uses = {ExpressionConverter.class, ValueSourceConverter.class})
public interface TemplateMappingDefinitionConverter {
    @Mapping(target = "scope", source = "scope", qualifiedByName = "convertToScope")
    @Mapping(target = "type", source = "type", qualifiedByName = "convertToType")
    TemplateMappingDefinition convertToModel(TemplateMappingDefinitionDto dto);

    @Mapping(target = "scope", source = "scope", qualifiedByName = "convertToScopeValue")
    @Mapping(target = "type", source = "type", qualifiedByName = "convertToTypeValue")
    TemplateMappingDefinitionDto convertToDto(TemplateMappingDefinition model);

    @Named("convertToScope")
    default MappingScope convertToScope(String value) {
        return value == null ? null : MappingScope.fromValue(value);
    }

    @Named("convertToScopeValue")
    default String convertToScopeValue(MappingScope value) {
        return value == null ? null : value.value();
    }

    @Named("convertToType")
    default TemplateValueType convertToType(String value) {
        return value == null ? null : TemplateValueType.fromValue(value);
    }

    @Named("convertToTypeValue")
    default String convertToTypeValue(TemplateValueType value) {
        return value == null ? null : value.value();
    }
}
