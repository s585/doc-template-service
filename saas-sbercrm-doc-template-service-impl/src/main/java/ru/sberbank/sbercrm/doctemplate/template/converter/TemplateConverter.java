package ru.sberbank.sbercrm.doctemplate.template.converter;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.sberbank.sbercrm.doctemplate.template.TemplateCreationRq;
import ru.sberbank.sbercrm.doctemplate.template.TemplateRs;
import ru.sberbank.sbercrm.doctemplate.template.TemplateUpdateRq;
import ru.sberbank.sbercrm.doctemplate.template.model.TemplateCreationCmd;
import ru.sberbank.sbercrm.doctemplate.template.model.Template;
import ru.sberbank.sbercrm.doctemplate.template.model.TemplateUpdateCmd;

@Mapper(componentModel = "spring", uses = {TemplateMappingConverter.class, RuleConverter.class})
public interface TemplateConverter {
    TemplateCreationCmd convertToModel(TemplateCreationRq request);

    @Mapping(target = "mappings", source = "templateMapping")
    TemplateUpdateCmd convertToModel(TemplateUpdateRq request);

    @Mapping(target = "format", expression = "java(template.getFormat() == null ? null : template.getFormat().value())")
    @Mapping(target = "templateMapping", source = "mappings")
    TemplateRs convertToRs(Template template);
}
