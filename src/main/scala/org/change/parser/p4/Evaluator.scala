package org.change.parser.p4

import org.change.parser.p4.control.queryimpl.{AbsValueWrapper, P4RootMemory, ScalarValue}
import org.change.parser.p4.control.{P4Memory, P4Query}
import z3.scala.{Z3Context, Z3Solver}

abstract class Evaluator[T<:P4Memory](p4Memory: P4Memory) {
  def hasResult: Boolean = ???
  def eval(expr : P4Query) : Option[P4Query] = ???
}

case class RootEvaluator(mem : P4RootMemory)
                        (implicit context : Z3Context) extends Evaluator[P4RootMemory](mem) {
  lazy val solver: Z3Solver = {
    val slv = context.mkSolver()
    slv.assertCnstr(mem.rootMemory.condition)
    slv
  }
  override lazy val hasResult: Boolean = {
    solver.check().get
  }
  def ever(expr : P4Query) : Boolean = {
    if (!hasResult) false
    else {
      val sv = expr.as[AbsValueWrapper].value.asInstanceOf[ScalarValue]
      solver.push()
      solver.assertCnstr(sv.z3AST)
      val res = solver.check().get
      solver.pop()
      res
    }
  }
  def never(expr : P4Query) : Boolean = !ever(expr)
  def always(expr : P4Query) : Boolean = never(!expr)
  override def eval(expr: P4Query): Option[P4Query] = {
    if (hasResult) {
      expr.as[AbsValueWrapper].value match {
        case sv : ScalarValue if sv.maybeBoolean.nonEmpty =>
          Some(mem.boolVal(sv.maybeBoolean.get))
        case sv : ScalarValue if sv.maybeInt.nonEmpty =>
          Some(mem.int(sv.maybeInt.get, sv.ofType))
      }
    } else {
      None
    }
  }
}
