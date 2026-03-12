package ru.sberbank.sbercrm.doctemplate.template.converter;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.sberbank.sbercrm.doctemplate.template.TemplateMappingDefinitionDto;
import ru.sberbank.sbercrm.doctemplate.template.model.MappingScope;
import ru.sberbank.sbercrm.doctemplate.template.model.TemplateMappingDefinition;
import ru.sberbank.sbercrm.doctemplate.template.model.TemplateValueType;

@Mapper(componentModel = "spring", uses = {ExpressionConverter.class, ValueSourceConverter.class})
public interface TemplateMappingDefinitionConverter {
    @Mapping(target = "scope", expression = "java(convertToScope(dto.getScope()))")
    @Mapping(target = "type", expression = "java(convertToType(dto.getType()))")
    TemplateMappingDefinition convertToModel(TemplateMappingDefinitionDto dto);

    @Mapping(target = "scope", expression = "java(convertToScopeValue(model.getScope()))")
    @Mapping(target = "type", expression = "java(convertToTypeValue(model.getType()))")
    TemplateMappingDefinitionDto convertToDto(TemplateMappingDefinition model);

    default MappingScope convertToScope(String value) {
        return value == null ? null : MappingScope.fromValue(value);
    }

    default String convertToScopeValue(MappingScope value) {
        return value == null ? null : value.value();
    }

    default TemplateValueType convertToType(String value) {
        return value == null ? null : TemplateValueType.fromValue(value);
    }

    default String convertToTypeValue(TemplateValueType value) {
        return value == null ? null : value.value();
    }
}
