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
  def apply(expression: Expression) : P4Query = expression match {
    case calculationRef: CalculationRef =>
      // TODO: calculation handling
      ctx.int(calculationRef.getPath.hashCode)
    case registerRef: RegisterRef =>
      //TODO: register handling
      ctx.int(registerRef.getPath.hashCode)
    case fr : FieldListRef =>
      ctx.int(fr.getFieldList.getName.hashCode)
    case c : LiteralExpr =>
      if (c.getWidth > 0)
        ctx.int(c.getValue, BVType(c.getWidth))
      else ctx.int(c.getValue)
    case pr : ParmRef =>
      val act = pr.getAction.getActionName + "_" +pr.getParameter.getParamName
      ctx.field(act)
    case ihr : IndexedHeaderRef =>
      if (ihr.isLast) {
        val path = ctx.field(ihr.getPath)
        ctx.field(ihr.getPath)(path.next())
      } else {
        val fld = ctx.field(ihr.getPath)
        fld(fld.next().int(ihr.getIndex))
      }
    case f : FieldRef => apply(f.getHeaderRef).field(f.getField)
    case r : HeaderRef => ctx.field(r.getPath)
  }
}
