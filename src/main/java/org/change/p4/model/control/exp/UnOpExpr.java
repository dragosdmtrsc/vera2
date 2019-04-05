package org.change.v2.p4.model.control.exp;

public class UnOpExpr implements P4Expr {
    /*'~' | '-'*/
    public enum OpType { NEG, NOT }
    private P4Expr expr;
    private OpType type;
    public static UnOpExpr from(String op, P4Expr expr) {
        UnOpExpr expression = new UnOpExpr();
        expression.expr = expr;
        if ("~".equals(op))
            expression.type = OpType.NOT;
        else if ("-".equals(op)) {
            expression.type = OpType.NEG;
        } else throw new IllegalArgumentException(op + " not supported as unary operator");
        return expression;
    }

    public P4Expr getExpr() {
        return expr;
    }

    public UnOpExpr setExpr(P4Expr expr) {
        this.expr = expr;
        return this;
    }

    public OpType getType() {
        return type;
    }

    public UnOpExpr setType(OpType type) {
        this.type = type;
        return this;
    }
}
