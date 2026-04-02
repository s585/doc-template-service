package ru.sberbank.sbercrm.saas.doctemplate.template.converter;

import org.mapstruct.Mapper;
import ru.sberbank.sbercrm.saas.doctemplate.template.dto.TemplateMappingDto;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.TemplateMapping;

@Mapper(componentModel = "spring", uses = TemplateMappingDefinitionConverter.class)
public interface TemplateMappingConverter {
    TemplateMapping convertToModel(TemplateMappingDto dto);

    TemplateMappingDto convertToDto(TemplateMapping model);
}
