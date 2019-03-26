package org.change.v2.p4.model.parser;

import org.change.v2.p4.model.control.exp.P4Expr;

/**
 * Created by dragos on 12.09.2017.
 */
public class SetStatement extends Statement {
    private FieldRef left;
    private Expression right;
    private P4Expr rightE;

    public FieldRef getLeft() {
        return left;
    }

    public Expression getRight() {
        return right;
    }

    public SetStatement(FieldRef left, Expression right) {
        this.left = left;
        this.right = right;
    }

    public P4Expr getRightE() {
        return rightE;
    }

    public SetStatement setRightE(P4Expr rightE) {
        this.rightE = rightE;
        return this;
    }

    @Override
    public String toString() {
        return left.getHeaderRef().getPath() + "." + left.getField() + " = " + getRight().toString();
    }
}
