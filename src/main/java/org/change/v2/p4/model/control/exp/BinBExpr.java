package org.change.v2.p4.model.control.exp;


public class BinBExpr implements P4BExpr {
    //'or' | 'and'
    public enum OpType { AND, OR }
    public static BinBExpr from(String op, P4BExpr l, P4BExpr r) {
        BinBExpr binBExpr = new BinBExpr();
        binBExpr.left = l;
        binBExpr.right = r;
        if ("or".equals(op)) binBExpr.type = OpType.OR;
        else if ("and".equals(op)) binBExpr.type = OpType.AND;
        else throw new IllegalArgumentException(op + " is not a valid boolean operator. " +
                    "Need 'or' or 'and'");
        return binBExpr;
    }
    private P4BExpr left, right;
    private OpType type;
    public P4BExpr getLeft() {
        return left;
    }
    public P4BExpr getRight() {
        return right;
    }
    public OpType getType() {
        return type;
    }

    @Override
    public String toString() {
        String ret = left.toString();
        switch (type) {
            case OR:
                ret += " or ";
            case AND:
                ret += " and ";
        }
        return ret + right.toString();
    }
}
