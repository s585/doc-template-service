package ru.sberbank.sbercrm.doctemplate.template.converter;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.sberbank.sbercrm.doctemplate.template.TemplateCreationRq;
import ru.sberbank.sbercrm.doctemplate.template.TemplateRs;
import ru.sberbank.sbercrm.doctemplate.template.model.TemplateCreationCmd;
import ru.sberbank.sbercrm.doctemplate.template.model.Template;

@Mapper(componentModel = "spring", uses = {TemplateMappingConverter.class, RuleConverter.class})
public interface TemplateConverter {
    TemplateCreationCmd convertToModel(TemplateCreationRq request);

    @Mapping(target = "format", expression = "java(template.getFormat() == null ? null : template.getFormat().value())")
    @Mapping(target = "templateMapping", source = "mappings")
    TemplateRs convertToRs(Template template);
}
