package org.change.v2.p4.model.control.exp;


public class NegBExpr implements P4BExpr {
    private P4BExpr expr;

    public NegBExpr(P4BExpr expr) {
        this.expr = expr;
    }

    public P4BExpr getExpr() {
        return expr;
    }

    @Override
    public String toString() {
        return "!" + expr.toString();
    }
}
