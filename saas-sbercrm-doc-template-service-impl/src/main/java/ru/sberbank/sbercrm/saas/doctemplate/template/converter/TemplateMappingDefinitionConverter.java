package ru.sberbank.sbercrm.saas.doctemplate.template.converter;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import ru.sberbank.sbercrm.saas.doctemplate.template.dto.TemplateMappingDefinitionDto;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.MappingScope;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.TemplateMappingDefinition;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.TemplateValueType;

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
        return MappingScope.fromValue(value);
    }

    @Named("convertToScopeValue")
    default String convertToScopeValue(MappingScope value) {
        return value.value();
    }

    @Named("convertToType")
    default TemplateValueType convertToType(String value) {
        return TemplateValueType.fromValue(value);
    }

    @Named("convertToTypeValue")
    default String convertToTypeValue(TemplateValueType value) {
        return value.value();
    }
}
