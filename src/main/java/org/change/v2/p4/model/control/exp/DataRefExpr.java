package org.change.v2.p4.model.control.exp;

import org.change.v2.p4.model.parser.DataRef;

public class DataRefExpr implements P4Expr {
    private DataRef dataRef;
    public DataRefExpr(DataRef dataRef) {
        this.dataRef = dataRef;
    }
    public DataRef getDataRef() {
        return dataRef;
    }

    public DataRefExpr setDataRef(DataRef dataRef) {
        this.dataRef = dataRef;
        return this;
    }
}
