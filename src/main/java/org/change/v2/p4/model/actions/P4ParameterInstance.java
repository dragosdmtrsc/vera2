package org.change.v2.p4.model.actions;

/**
 * Created by dragos on 30.08.2017.
 */
public class P4ParameterInstance {
    private P4ActionParameter parameter;
    private String value;

    public String getValue() {
        return value;
    }

    public P4ParameterInstance setValue(String value) {
        this.value = value;
        return this;
    }

    public P4ActionParameter getParameter() {
        return parameter;
    }

    public P4ParameterInstance setParameter(P4ActionParameter parameter) {
        this.parameter = parameter;
        return this;
    }

    @Override
    public String toString() {
        return "P4ParameterInstance{" +
                "parameter=" + parameter +
                ", value='" + value + '\'' +
                '}';
    }
}
