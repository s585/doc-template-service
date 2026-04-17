package ru.sberbank.sbercrm.saas.doctemplate.document.service.context.expression;

import ru.sberbank.sbercrm.saas.doctemplate.template.model.TemplateMapping;

public interface ExpressionEvaluator {
    Object evaluate(TemplateMapping mapping, Object sourceValue);
}
