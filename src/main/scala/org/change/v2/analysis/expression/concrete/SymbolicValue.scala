package org.change.v2.analysis.expression.concrete

import org.change.v2.analysis.expression.abst.{Expression, FloatingExpression}
import org.change.v2.analysis.memory.{Intable, State}
import org.change.v2.analysis.z3.Z3Util
import z3.scala.{Z3AST, Z3Solver}

/**
  * Created by radu on 3/24/15.
  */
case class SymbolicValue(name: String = "") extends Expression with FloatingExpression {
  def ast = Z3Util.z3Context.mkConst(s"sym${id.toString}", Z3Util.defaultSort)
  def canonicalName(): String = if (name == "") s"sym$id" else name

  override def equals(o: scala.Any): Boolean = o match {
    case sym : SymbolicValue => (name != "" && sym.name == name) || sym.id == id
    case _ => false
  }

  override def toZ3(solver: Option[Z3Solver] = None): (Z3AST, Option[Z3Solver]) = (ast, solver)

  /**
    * A floating expression may include unbounded references (e.g. symbol ids)
    *
    * Given a context (the state) it can produce an evaluable expression.
    *
    * @param s
    * @return
    */
  override def instantiate(s: State): Either[Expression, String] = Left(this)

  override def toString = if (name == "") s"[Symbolic #$id]" else "[Symbolic " + name + "]"
}

case class Havoc(prefix : String) extends FloatingExpression {
  override def instantiate(s: State): Either[Expression, String] = ???
}
case class Take(lv : Either[String, Intable], from : Int, width : Int) extends FloatingExpression {
  override def instantiate(s: State): Either[Expression, String] = ???
}
case class Extract(e : Expression, from : Int, len : Int) extends Expression {
  override def toZ3(solver: Option[Z3Solver]): (Z3AST, Option[Z3Solver]) = ???
}
case class Concat(exprs : Iterable[Expression]) extends Expression {
  override def toZ3(solver: Option[Z3Solver]): (Z3AST, Option[Z3Solver]) = ???
}