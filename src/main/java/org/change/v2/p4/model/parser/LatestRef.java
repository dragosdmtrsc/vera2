package org.change.v2.p4.model.parser;

/**
 * Created by dragos on 12.09.2017.
 */
public class LatestRef extends Expression {
    private String fieldName;

    public String getFieldName() {
        return fieldName;
    }

    public LatestRef(String fieldName) {
        this.fieldName = fieldName;
    }
}
