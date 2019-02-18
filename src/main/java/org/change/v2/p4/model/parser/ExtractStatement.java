package org.change.v2.p4.model.parser;

/**
 * Created by dragos on 12.09.2017.
 */
public class ExtractStatement extends Statement {
    private HeaderRef expression;
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

    public HeaderRef getExpression() {
        return expression;
    }

    public ExtractStatement(HeaderRef expression) {
        this.expression = expression;
    }

    @Override
    public String toString() {
        return "extract(" + expression.getPath() + ")";
    }
}
