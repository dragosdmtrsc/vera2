package org.change.v2.p4.model.actions;

/**
 * Created by dragos on 30.08.2017.
 */
public class P4ActionParameter {
    private int type;
    private String paramName;

    public P4ActionParameter(int type, String paramName) {
        this.type = type;
        this.paramName = paramName;
    }

    public P4ActionParameter(String paramName) {
        this(P4ActionParameterType.UNKNOWN.x, paramName);
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getParamName() {
        return paramName;
    }

    public void setParamName(String paramName) {
        this.paramName = paramName;
    }

    @Override
    public String toString() {
        return "P4ActionParameter{" +
                "type=" + type +
                ", paramName='" + paramName + '\'' +
                '}';
    }
}
