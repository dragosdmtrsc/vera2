package org.change.v2.p4.model.control.exp;

import java.util.Objects;

public class LiteralBool implements P4BExpr {
    private boolean v;
    private LiteralBool(boolean v) {
        this.v = v;
    }
    private static LiteralBool FF = new LiteralBool(false), TT = new LiteralBool(true);
    public static LiteralBool falseLit() {
        return FF;
    }
    public static LiteralBool trueLit() {
        return TT;
    }
    public static LiteralBool fromValue(boolean v) {
        if (v) return TT; return FF;
    }
    public boolean value() {
        return v;
    }

    @Override
    public String toString() {
        return Objects.toString(v);
    }
}
