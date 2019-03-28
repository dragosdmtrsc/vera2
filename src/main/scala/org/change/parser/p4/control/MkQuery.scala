package org.change.parser.p4.control

import org.change.v2.p4.model.control.exp.{P4BExpr, P4Expr}
import org.change.v2.p4.model.parser.{Expression, FieldRef}

import scala.collection.mutable

object MkQuery {
  def apply(context : P4Memory,
            expr : P4Expr) : P4Query = {
    val where = mutable.Map.empty[Object, P4Query]
    Traverse(QueryMaker(context, where))(expr)
    where(expr)
  }
  def apply(context : P4Memory,
            expr : P4BExpr) : P4Query = {
    val where = mutable.Map.empty[Object, P4Query]
    Traverse(QueryMaker(context, where))(expr)
    where(expr)
  }
  //TODO do it right
  def validityFailure(context : P4Memory, expr: P4Expr) : P4Query =
    context.boolVal(true)
  def validityFailure(context : P4Memory, fieldRef: FieldRef) : P4Query = context.boolVal(true)
  def validityFailure(context : P4Memory, expr: P4BExpr) : P4Query = context.boolVal(true)
  def validityFailure(context : P4Memory, expr: Expression) : P4Query = context.boolVal(true)
}
