package org.change.parser.p4.control

import org.change.v2.p4.model.control.exp.FieldRefExpr
import org.change.v2.p4.model.parser.{ExtractStatement, HeaderRef, IndexedHeaderRef, State}

class ResolveLatest extends ASTVisitor {
  var latest : ExtractStatement = null

  override def postorder(ps: State) : Unit =
    latest = null

  override def preorder(extractStatement: ExtractStatement) : Boolean = {
    latest = extractStatement
    //block propagation in extracts
    false
  }

  override def postorder(fieldRefExpr : FieldRefExpr) : Unit = {
    if (fieldRefExpr.getFieldRef.getHeaderRef == null) {
      if (latest != null) {
        val h = latest.getExpression match {
          case ihr : IndexedHeaderRef if ihr.isNext =>
            new IndexedHeaderRef(ihr).setLast()
          case _ => latest.getExpression
        }
        fieldRefExpr.getFieldRef.setHeaderRef(h)
      } else {
        throw new IllegalStateException(s"unknown reference to latest in $fieldRefExpr")
      }
    }
  }
}
