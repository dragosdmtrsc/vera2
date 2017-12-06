package org.change.v2.analysis.expression.concrete.nonprimitive

import org.change.v2.analysis.expression.abst.{Expression, FloatingExpression}
import org.change.v2.analysis.memory.{State, Value}
import org.change.v2.analysis.z3.Z3Util
import z3.scala.{Z3AST, Z3Solver}

/**
  * Created by dragos on 11.09.2017.
  */
case class LXor(a: Value, b: Value) extends Expression {
  override def toZ3(solver: Option[Z3Solver] = None): (Z3AST, Option[Z3Solver]) = {
    throw new UnsupportedOperationException("Not implemented in the old fashioned SEFL translation. Use executors")
  }

  override def toString = s"(${a.e} & ${b.e})"
}

case class :^:(left: FloatingExpression, right: FloatingExpression) extends FloatingExpression {
  /**
    * A floating expression may include unbounded references (e.g. symbol ids)
    *
    * Given a context (the state) it can produce an evaluable expression.
    * @param s
    * @return
    */
  override def instantiate(s: State): Either[Expression, String] = {
    (left instantiate s) match {
      case Left(e1) => (right instantiate s) match {
        case Left(e2) => Left(LXor(Value(e1), Value(e2)))
        case cause2@Right(_) => cause2
      }
      case cause1@Right(_) => cause1
    }
  }
}

