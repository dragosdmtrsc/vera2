package org.change.v3.syntax

trait BVExpr extends Expr
case class BVAdd(a : BVExpr, b : BVExpr) extends BVExpr
case class BVSub(a : BVExpr, b : BVExpr) extends BVExpr
case class BVAnd(a : BVExpr, b : BVExpr) extends BVExpr
case class BVOr(a : BVExpr, b : BVExpr) extends BVExpr
case class BVNot(b : BVExpr) extends BVExpr
case class BVNeg(a : BVExpr) extends BVExpr
case class Havoc(prefix : String) extends BVExpr

trait LVExpr extends Expr
case class Symbol(path : String) extends LVExpr with BVExpr
case class Reference(intable : Intable) extends LVExpr with BVExpr