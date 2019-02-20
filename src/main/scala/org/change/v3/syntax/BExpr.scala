package org.change.v3.syntax

trait BExpr extends Expr

case class LAnd(components : Iterable[BExpr]) extends BExpr
case class LOr(components : Iterable[BExpr]) extends BExpr
case class LNot(what : BExpr) extends BExpr

case class EQ(a : BVExpr, b : BVExpr) extends BExpr
case class LT(a : BVExpr, b : BVExpr) extends BExpr