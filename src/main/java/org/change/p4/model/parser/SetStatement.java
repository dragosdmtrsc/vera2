package org.change.p4.model.parser;

import org.change.p4.model.control.exp.P4Expr;

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
    public SetStatement(SetStatement ss) {
        this.left = ss.left;
        this.right = ss.right;
        this.rightE = ss.rightE;
    }

    public SetStatement setLeft(FieldRef left) {
        this.left = left;
        return this;
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
        return left + " = " + getRightE().toString();
    }
}
