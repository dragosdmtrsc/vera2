package org.change.p4.control

import org.change.p4.model.control.exp.{P4BExpr, P4Expr}
import org.change.p4.model.parser._

case class RichContext(ctx: P4Memory) {
  def indexOutOfBounds(p4Expr: P4Expr): P4Query = {
    MkQuery.indexOutOfBounds(ctx, p4Expr)
  }
  def indexOutOfBounds(p4Expr: P4BExpr): P4Query = {
    MkQuery.indexOutOfBounds(ctx, p4Expr)
  }
  def indexOutOfBounds(expr: Expression): P4Query = {
    MkQuery.indexOutOfBounds(ctx, expr)
  }
  def validityFailure(p4Expr: P4Expr): P4Query = {
    MkQuery.validityFailure(ctx, p4Expr)
  }
  def validityFailure(p4Expr: P4BExpr): P4Query = {
    MkQuery.validityFailure(ctx, p4Expr)
  }
  def validityFailure(expr: Expression): P4Query = {
    MkQuery.validityFailure(ctx, expr)
  }
  def apply(p4Expr: P4BExpr): P4Query = {
    MkQuery(ctx, p4Expr)
  }
  def apply(p4Expr: P4Expr): P4Query = {
    MkQuery(ctx, p4Expr)
  }
  def apply(expression: Expression): P4Query = MkQuery.apply(ctx, expression)
}
