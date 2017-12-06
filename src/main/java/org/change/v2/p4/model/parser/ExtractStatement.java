package org.change.v2.p4.model.parser;

/**
 * Created by dragos on 12.09.2017.
 */
public class ExtractStatement extends Statement {
    private Expression expression;
    private int crt, width;
    public ExtractStatement setLocation(int crt, int width) {
        ExtractStatement es = new ExtractStatement(expression);
        es.crt = crt;
        es.width = width;
        return es;
    }

    public int getCrt() {
        return crt;
    }

    public int getWidth() {
        return width;
    }

    public ExtractStatement setCrt(int crt) {
        this.crt = crt;
        return this;
    }

    public ExtractStatement setWidth(int width) {
        this.width = width;
        return this;
    }

    public Expression getExpression() {
        return expression;
    }

    public ExtractStatement(Expression expression) {
        this.expression = expression;
    }
}
