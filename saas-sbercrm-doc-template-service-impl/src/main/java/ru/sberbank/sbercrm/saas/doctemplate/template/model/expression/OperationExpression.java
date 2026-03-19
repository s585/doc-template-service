package ru.sberbank.sbercrm.saas.doctemplate.template.model.expression;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.sberbank.sbercrm.saas.doctemplate.template.constant.TemplateConstants;

import java.util.List;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@JsonTypeName(TemplateConstants.ExpressionJsonTypes.OPERATION)
@JsonPropertyOrder({"type", "op", "args"})
public final class OperationExpression implements Expression {
    private ExpressionOperator op;
    private List<Expression> args;

    @Override
    public String getType() {
        return TemplateConstants.ExpressionJsonTypes.OPERATION;
    }
}
