package ru.sberbank.sbercrm.saas.doctemplate.document.service.context;

import java.util.LinkedHashSet;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.sberbank.sbercrm.saas.doctemplate.shared.dto.SelectDto;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.Template;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.TemplateMapping;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.source.DirectValueSource;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.source.ReferenceValueSource;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.source.ValueSource;

/**
 * Формирует {@link SelectDto} для загрузки основного бизнес-объекта перед генерацией.
 *
 * <p>В select попадают только поля, необходимые mapping-ам шаблона: прямые source paths и поля,
 * по которым затем будут найдены reference collections.
 */
@Component
@RequiredArgsConstructor
public class GenerationSelectBuilder {
    private final GenerationPathResolver generationPathResolver;

    /**
     * Собирает уникальный набор полей бизнес-объекта, нужных для всех mapping-ов шаблона.
     */
    public SelectDto build(Template template) {
        Set<String> fields = new LinkedHashSet<>();
        if (template.getMappings() == null) {
            return SelectDto.EMPTY;
        }

        for (TemplateMapping mapping : template.getMappings()) {
            if (mapping.getDefinition() == null || mapping.getDefinition().getSource() == null) {
                continue;
            }
            collectFields(fields, mapping);
        }

        return SelectDto.builder()
            .fields(fields)
            .build();
    }

    private void collectFields(Set<String> fields, TemplateMapping mapping) {
        ValueSource source = mapping.getDefinition().getSource();
        if (source instanceof DirectValueSource directSource) {
            fields.add(generationPathResolver.normalizeSourcePath(directSource.getPath(), mapping.getKey()));
            return;
        }
        if (source instanceof ReferenceValueSource referenceSource) {
            fields.add(generationPathResolver.normalizeSourcePath(referenceSource.getReferenceValuePath(), mapping.getKey()));
            fields.add(generationPathResolver.normalizeSourcePath(referenceSource.getTargetPath(), mapping.getKey()));
        }
    }
}
