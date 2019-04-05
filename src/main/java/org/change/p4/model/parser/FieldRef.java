package org.change.p4.model.parser;

import org.change.p4.model.Field;

public class FieldRef extends Expression {
  private HeaderRef headerRef;
  private String field;
  private Field fieldReference;

  public HeaderRef getHeaderRef() {
    return headerRef;
  }

  public FieldRef setHeaderRef(HeaderRef headerRef) {
    this.headerRef = headerRef;
    return this;
  }

  public Field getFieldReference() {
    return fieldReference;
  }

  public FieldRef setFieldReference(Field fieldReference) {
    this.fieldReference = fieldReference;
    return this.setField(fieldReference.getName());
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
    return ((headerRef != null) ? headerRef.toString() : "latest") +
            "." + field;
  }
}
