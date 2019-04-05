package org.change.v2.p4.model.parser;

import org.change.v2.p4.model.control.ControlStatement;

public class EmitStatement implements ControlStatement {
    private HeaderRef headerRef;
    public EmitStatement(HeaderRef headerRef) {
        this.headerRef = headerRef;
    }
    public HeaderRef getHeaderRef() {
        return headerRef;
    }
    public EmitStatement setHeaderRef(HeaderRef headerRef) {
        this.headerRef = headerRef;
        return this;
    }

    @Override
    public String toString() {
        return "emit(" + headerRef + ")";
    }
}
