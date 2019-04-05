package org.change.p4.model.parser;

public class FieldListRef extends Ref {
  private org.change.p4.model.fieldlists.FieldList fieldList;

  public org.change.p4.model.fieldlists.FieldList getFieldList() {
    return fieldList;
  }

  public FieldListRef setFieldList(org.change.p4.model.fieldlists.FieldList fieldList) {
    this.fieldList = fieldList;
    return this;
  }
}
