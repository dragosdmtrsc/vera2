package org.change.v2.p4.model.parser;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by dragos on 12.09.2017.
 */
public class CaseEntry extends Statement {
    private List<Expression> expressions = new ArrayList<Expression>();
    private ReturnStatement returnStatement;
    private List<Value> values = new ArrayList<Value>();

    public CaseEntry addExpression(Expression expression) {
        this.expressions.add(expression);
        return this;
    }

    public List<Expression> getExpressions() {
        return expressions;
    }

    public ReturnStatement getReturnStatement() {
        return returnStatement;
    }

    public CaseEntry setReturnStatement(ReturnStatement returnStatement) {
        this.returnStatement = returnStatement;
        return this;
    }

    public List<Value> getValues() {
        return values;
    }
    public CaseEntry addValue(Value v) {
        this.values.add(v);
        return this;
    }
}
