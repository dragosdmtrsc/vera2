package org.change.v2.p4.model.actions;

import org.change.p4.control.types.P4Type;

/**
 * Created by dragos on 30.08.2017.
 */
public class P4ActionParameter {
    private P4Type sort;
    private int type;
    private String paramName;
    private boolean isLeftValue = false;

    public P4ActionParameter(int type, String paramName) {
        this.type = type;
        this.isLeftValue = P4ActionParameterType.isLV(type);
        this.paramName = paramName;
    }

    public boolean isLeftValue() {
        return isLeftValue;
    }
    public boolean isRightValue() {
        return !isLeftValue;
    }

    public P4ActionParameter setLeftValue(boolean leftValue) {
        isLeftValue = leftValue;
        return this;
    }
    public P4ActionParameter setLV() {
        return setLeftValue(true);
    }
    public P4ActionParameter setRV() {
        return setLeftValue(false);
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

    public P4Type getSort() {
        return sort;
    }

    public P4ActionParameter setSort(P4Type sort) {
        this.sort = sort;
        return this;
    }

    @Override
    public String toString() {
        return "P4ActionParameter{" +
                "type=" + type +
                ", paramName='" + paramName + '\'' +
                '}';
    }
}
