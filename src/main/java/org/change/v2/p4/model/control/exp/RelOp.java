package org.change.v2.p4.model.control.exp;

public class RelOp implements P4BExpr {
    //'>' | '>=' | '==' | '<=' | '<' | '!='
    public enum OpType { GT, GTE, EQ, LTE, LT, NE;
        @Override
        public String toString() {
            switch (this) {
                case EQ: return "==";
                case GT: return ">";
                case LT: return "<";
                case NE: return "!=";
                case GTE: return ">=";
                case LTE: return "<=";
            }
            return "";
        }
    }
    private P4Expr left, right;
    private OpType type;
    public static RelOp from(String op, P4Expr l, P4Expr r) {
        RelOp relOp = new RelOp();
        relOp.left = l;
        relOp.right = r;
        if (">".equals(op)) {
            relOp.type = OpType.GT;
        } else if (">=".equals(op)) {
            relOp.type = OpType.GTE;
        } else if ("==".equals(op)) {
            relOp.type = OpType.EQ;
        } else if ("<=".equals(op)) {
            relOp.type = OpType.LTE;
        } else if ("<".equals(op)) {
            relOp.type = OpType.LT;
        } else if ("!=".equals(op)) {
            relOp.type = OpType.NE;
        } else {
            throw new IllegalArgumentException(op + " is not a valid relop");
        }
        return relOp;
    }

    public P4Expr getLeft() {
        return left;
    }
    public P4Expr getRight() {
        return right;
    }
    public OpType getType() {
        return type;
    }

    @Override
    public String toString() {
        return getLeft().toString() + getType() + getRight();
    }
}
