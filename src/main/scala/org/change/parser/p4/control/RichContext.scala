package org.change.parser.p4.control

import org.change.plugins.vera.BVType
import org.change.v2.p4.model.control.exp.{FieldRefExpr, LiteralExpr, P4BExpr, P4Expr}
import org.change.v2.p4.model.parser._

case class RichContext(ctx : P4Memory) {
  def validityFailure(p4Expr: P4Expr) : P4Query = {
    MkQuery.validityFailure(ctx, p4Expr)
  }
  def validityFailure(p4Expr: P4BExpr) : P4Query = {
    MkQuery.validityFailure(ctx, p4Expr)
  }
  def validityFailure(expr: Expression) : P4Query = {
    MkQuery.validityFailure(ctx, expr)
  }
  def validityFailure(fref: FieldRef) : P4Query = {
    MkQuery.validityFailure(ctx, fref)
  }
  def apply(p4Expr: P4BExpr) : P4Query = {
    MkQuery(ctx, p4Expr)
  }
  def apply(p4Expr: P4Expr) : P4Query = {
    MkQuery(ctx, p4Expr)
  }
  def apply(expression: Expression) : P4Query = MkQuery.apply(ctx, expression)
}
