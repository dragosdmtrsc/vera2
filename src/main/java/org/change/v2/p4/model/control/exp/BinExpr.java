package org.change.v2.p4.model.control.exp;


import java.util.HashMap;
import java.util.Map;

public class BinExpr implements P4Expr {
    /*'+' | '*' | '-' | '<<' | '>>' | '&' | '|' | '^'*/
    public enum OpType { PLUS, TIMES, MINUS, SHL, SHR, BVAND, BVOR, BVXOR }
    private static Map<String, OpType> all = new HashMap<>();
    private P4Expr left, right;
    private OpType type;
    private static String toString(OpType ot) {
        switch (ot) {
            case PLUS:
                return "+";
            case TIMES:
                return "*";
            case MINUS:
                return "-";
            case SHL:
                return "<<";
            case SHR:
                return ">>";
            case BVAND:
                return "&";
            case BVOR:
                return "|";
            case BVXOR:
                return "^";
        }
        return "";
    }
    public static BinExpr bvand(P4Expr l, P4Expr r) {
        BinExpr binExpr = new BinExpr();
        binExpr.left = l;
        binExpr.right = r;
        binExpr.type = OpType.BVAND;
        return binExpr;
    }
    public static BinExpr from(String op, P4Expr l, P4Expr r) {
        BinExpr binExpr = new BinExpr();
        binExpr.left = l;
        binExpr.right = r;
        if ("+".equals(op)) {
            binExpr.type = OpType.PLUS;
        } else if ("*".equals(op)) {
            binExpr.type = OpType.TIMES;
        } else if ("-".equals(op)) {
            binExpr.type = OpType.MINUS;
        } else if ("<<".equals(op)) {
            binExpr.type = OpType.SHL;
        } else if (">>".equals(op)) {
            binExpr.type = OpType.SHR;
        } else if ("&".equals(op)) {
            binExpr.type = OpType.BVAND;
        } else if ("|".equals(op)) {
            binExpr.type = OpType.BVOR;
        } else if ("^".equals(op)) {
            binExpr.type = OpType.BVXOR;
        } else {
            throw new IllegalArgumentException(op + " is not a supported binary operator");
        }
        return binExpr;
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
        return getLeft().toString() + " " +
                toString(getType()) + " " + getRight().toString();
    }
}
