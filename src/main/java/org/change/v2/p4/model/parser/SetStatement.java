package org.change.v2.p4.model.parser;

/**
 * Created by dragos on 12.09.2017.
 */
public class SetStatement extends Statement {
    private Expression left, right;

    public Expression getLeft() {
        return left;
    }

    public Expression getRight() {
        return right;
    }

    public SetStatement(Expression left, Expression right) {
        this.left = left;
        this.right = right;
    }
}
