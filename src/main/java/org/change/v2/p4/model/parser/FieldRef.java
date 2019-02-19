package org.change.v2.p4.model.parser;

public class FieldRef extends Expression {
    private HeaderRef headerRef;
    private String field;

    public HeaderRef getHeaderRef() {
        return headerRef;
    }

    public FieldRef setHeaderRef(HeaderRef headerRef) {
        this.headerRef = headerRef;
        return this;
    }

    public String getField() {
        return field;
    }

    public FieldRef setField(String field) {
        this.field = field;
        return this;
    }

    @Override
    public String toString() {
        return headerRef.toString() + "." + field;
    }
}
