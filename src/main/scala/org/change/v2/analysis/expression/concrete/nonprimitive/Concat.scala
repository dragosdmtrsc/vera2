package org.change.v2.analysis.expression.concrete.nonprimitive

import org.change.v2.analysis.expression.abst.Expression
import org.change.v2.analysis.memory.MemoryObject
import z3.scala.Z3Solver

case class Concat(expressions : List[MemoryObject]) extends Expression {
  override def toZ3(solver: Option[Z3Solver]) = ???
}
