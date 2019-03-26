package org.change.parser.p4.control

import org.change.v2.p4.model.control.exp.FieldRefExpr
import org.change.v2.p4.model.parser.{ExtractStatement, State}

class ResolveLatest extends ASTVisitor {
  var latest : ExtractStatement = null

  override def postorder(ps: State) : Unit =
    latest = null

  override def postorder(extractStatement: ExtractStatement) : Unit =
    latest = extractStatement

  override def postorder(fieldRefExpr : FieldRefExpr) : Unit = {
    if (fieldRefExpr.getFieldRef.getHeaderRef == null) {
      if (latest != null)
        fieldRefExpr.getFieldRef.setHeaderRef(latest.getExpression)
      else {
        throw new IllegalStateException(s"unknown reference to latest in $fieldRefExpr")
      }
    }
  }
}
