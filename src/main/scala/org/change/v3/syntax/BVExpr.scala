package org.change.v3.syntax

trait BVExpr extends Expr
case class BVAdd(a : BVExpr, b : BVExpr) extends BVExpr
case class BVSub(a : BVExpr, b : BVExpr) extends BVExpr
case class BVAnd(a : BVExpr, b : BVExpr) extends BVExpr
case class BVShl(a : BVExpr, b : BVExpr) extends BVExpr
case class BVOr(a : BVExpr, b : BVExpr) extends BVExpr
object BVXor {
  def apply(a : BVExpr, b : BVExpr) : BVExpr = {
    BVAnd(
      BVOr(a, b),
      BVNot(BVAnd(a, b))
    )
  }
}

case class BVNot(b : BVExpr) extends BVExpr
case class BVNeg(a : BVExpr) extends BVExpr
case class Havoc(prefix : String) extends BVExpr
case class Literal(v: BigInt) extends BVExpr

trait LVExpr extends Expr
case class Symbol(path : String) extends LVExpr with BVExpr
case class Reference(intable : Intable) extends LVExpr with BVExpr
//case class ArrayIndex(path : String, index: Expr) extends LVExpr