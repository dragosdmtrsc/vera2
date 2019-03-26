package org.change.v2.p4.model.parser;

import org.change.v2.p4.model.actions.P4ActionParameter;

public class ParmRef extends Ref {
    private P4ActionParameter parameter;
    public ParmRef(P4ActionParameter parameter) {
        this.parameter = parameter;
        setPath(parameter.getParamName());
    }

    public P4ActionParameter getParameter() {
        return parameter;
    }

    public ParmRef setParameter(P4ActionParameter parameter) {
        this.parameter = parameter;
        return this;
    }
}