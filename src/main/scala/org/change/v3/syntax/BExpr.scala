package org.change.v3.syntax

trait BExpr extends Expr
case class BoolLiteral(v : Boolean) extends BExpr
object True extends BoolLiteral(true)
object False extends BoolLiteral(false)
case class LAnd(a : BExpr, b : BExpr) extends BExpr
case class LOr(a : BExpr, b : BExpr) extends BExpr
case class LNot(what : BExpr) extends BExpr

case class EQ(a : BVExpr, b : BVExpr) extends BExpr
case class LT(a : BVExpr, b : BVExpr) extends BExpr
case class GT(a : BVExpr, b : BVExpr) extends BExpr
case class LTE(a : BVExpr, b : BVExpr) extends BExpr
case class GTE(a : BVExpr, b : BVExpr) extends BExpr