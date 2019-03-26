package org.change.v2.p4.model.parser;

import org.change.v2.p4.model.control.exp.P4BExpr;
import org.change.v2.p4.model.control.exp.P4Expr;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by dragos on 12.09.2017.
 */
public class CaseEntry extends Statement {
    private boolean def = false;
    private List<Expression> expressions = new ArrayList<>();
    private List<P4Expr> bvexpressions = new ArrayList<>();
    private List<CaseEntry> negated = new ArrayList<>();
    private ReturnStatement returnStatement;
    private List<Value> values = new ArrayList<Value>();
    public CaseEntry() {}
    public CaseEntry(CaseEntry ce) {
        def = ce.def;
        negated = ce.negated;
        expressions = ce.expressions;
        bvexpressions = ce.bvexpressions;
        returnStatement = ce.returnStatement;
        values = ce.values;
    }
    public List<CaseEntry> getNegated() {
        return negated;
    }
    public CaseEntry setNegated(List<CaseEntry> negated) {
        this.negated = negated;
        return this;
    }

    public CaseEntry addExpression(P4Expr expr) {
        bvexpressions.add(expr);
        return this;
    }
    public List<P4Expr> getBVExpressions() {
        return bvexpressions;
    }

    public CaseEntry addExpression(Expression expression) {
        this.expressions.add(expression);
        return this;
    }
    public CaseEntry setDefault() { def = true; return this; }
    public boolean isDefault() { return def; }

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

    @Override
    public String toString() {
        return "CaseEntry{" +
                "expressions=" + expressions +
                ", returnStatement=" + returnStatement +
                '}';
    }
}
