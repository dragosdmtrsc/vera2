package org.change.p4.model.control.exp;

import org.change.p4.model.parser.FieldRef;

public class FieldRefExpr implements P4Expr {
  private FieldRef fieldRef;

  public FieldRefExpr(FieldRef fieldRef) {
    this.fieldRef = fieldRef;
  }

  public FieldRef getFieldRef() {
    return fieldRef;
  }

  @Override
  public String toString() {
    return fieldRef.toString();
  }
}
