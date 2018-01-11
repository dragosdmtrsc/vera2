package org.change.v2.p4.model.parser;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Created by dragos on 12.09.2017.
 */
public class CompoundExpression extends Expression {
    private boolean isPlus = false;
    private Expression left;
    private Expression right;

    public CompoundExpression(boolean isPlus, Expression left, Expression right) {
        this.isPlus = isPlus;
        this.left = left;
        this.right = right;
    }

    public boolean isPlus() {
        return isPlus;
    }

    public Expression getLeft() {
        return left;
    }

    public Expression getRight() {
        return right;
    }
}
