package org.change.v2.p4.model.parser;

public class FieldListRef extends Ref {
    private org.change.v2.p4.model.fieldlists.FieldList fieldList;

    public org.change.v2.p4.model.fieldlists.FieldList getFieldList() {
        return fieldList;
    }

    public FieldListRef setFieldList(org.change.v2.p4.model.fieldlists.FieldList fieldList) {
        this.fieldList = fieldList;
        return this;
    }
}
