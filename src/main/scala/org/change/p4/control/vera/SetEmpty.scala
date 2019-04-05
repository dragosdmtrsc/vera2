package org.change.plugins.vera

import org.change.p4.control.ASTVisitor
import org.change.v2.p4.model.control.exp.FieldRefExpr
import org.change.v2.p4.model.parser.HeaderRef

class SetEmpty(headerRef: HeaderRef) extends ASTVisitor {
  override def postorder(fieldRefExpr: FieldRefExpr): Unit = {
    if (fieldRefExpr.getFieldRef.getHeaderRef == null) {
      System.err.println("non null " +
        fieldRefExpr.getFieldRef + " " +
        fieldRefExpr.getFieldRef.hashCode())
      fieldRefExpr.getFieldRef.setHeaderRef(headerRef)
    }
  }
}
