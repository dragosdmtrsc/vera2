package org.change.v2.p4.model.parser;

import org.change.v2.p4.model.control.exp.*;
import org.change.v3.syntax.Literal;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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
    private List<LiteralExpr> bvValues = new ArrayList<>();
    private List<LiteralExpr> bvMasks = new ArrayList<>();
    public CaseEntry() {}
    public CaseEntry(CaseEntry ce) {
        def = ce.def;
        negated = ce.negated;
        expressions = ce.expressions;
        bvexpressions = ce.bvexpressions;
        returnStatement = ce.returnStatement;
        values = ce.values;
        bvValues = ce.bvValues;
        bvMasks = ce.bvMasks;
    }
    public List<CaseEntry> getNegated() {
        return negated;
    }
    public CaseEntry setNegated(List<CaseEntry> negated) {
        this.negated = negated;
        return this;
    }

    public P4BExpr getBExpr() {
        if (isDefault()) {
            P4BExpr nxt = LiteralBool.trueLit();
            for (CaseEntry n : negated) {
                nxt = BinBExpr.from("and", new NegBExpr(n.getBExpr()), nxt);
            }
            return nxt;
        } else {
            int sz = bvexpressions.size();
            if (sz == bvValues.size()) {
                P4BExpr nxt = LiteralBool.trueLit();
                for (int i = 0; i < sz; ++i) {
                    P4Expr crt = bvexpressions.get(i);
                    P4Expr val = bvValues.get(i);
                    P4Expr mask = bvMasks.get(i);
                    P4BExpr thiscase = RelOp.from(
                            "==",
                            BinExpr.from("&", crt, mask),
                            BinExpr.from("&", val, mask)
                    );
                    nxt = BinBExpr.from("and", nxt, thiscase);
                }
                return nxt;
            } else {
                throw new AssertionError(bvValues + " should be of length " + bvexpressions.size());
            }
        }
    }

    public CaseEntry addExpression(P4Expr expr) {
        bvexpressions.add(expr);
        return this;
    }
    public List<P4Expr> getBVExpressions() {
        return bvexpressions;
    }

    public List<LiteralExpr> getBvValues() {
        return bvValues;
    }

    public CaseEntry setBvValues(List<LiteralExpr> bvValues) {
        this.bvValues = bvValues;
        return this;
    }

    public List<LiteralExpr> getBvMasks() {
        return bvMasks;
    }

    public CaseEntry setBvMasks(List<LiteralExpr> bvMasks) {
        this.bvMasks = bvMasks;
        return this;
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
        if (!isDefault())
            return "case(" + bvValues + "&" + bvMasks + ")";
        return "default";
    }
}
