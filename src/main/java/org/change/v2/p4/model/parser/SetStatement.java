package org.change.v2.p4.model.parser;

/**
 * Created by dragos on 12.09.2017.
 */
public class SetStatement extends Statement {
    private FieldRef left;
    private Expression right;

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

    @Override
    public String toString() {
        return left.getHeaderRef().getPath() + "." + left.getField() + " = " + getRight().toString();
    }
}
