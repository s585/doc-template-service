package ru.sberbank.sbercrm.saas.doctemplate.document.service.context.expression;

import org.springframework.stereotype.Service;
import ru.sberbank.sbercrm.saas.doctemplate.template.model.TemplateMapping;

@Service
public class NoOpExpressionEvaluator implements ExpressionEvaluator {
    @Override
    public Object evaluate(TemplateMapping mapping, Object sourceValue) {
        // Skeleton: the expression pipeline is now explicit, but evaluation is no-op for now.
        return sourceValue;
    }
}
