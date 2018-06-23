package org.change.v2.analysis.memory

import org.change.v2.analysis.constraint.Condition
import org.change.v2.analysis.expression.abst.Expression
import org.change.v2.analysis.processingmodels.Instruction

object AllocationState {
  val NALLOC = 0
  val ALLOC = 1
  val ASSIGN = 2
}

case class State2(tags : Map[String, Int],
                  rawObjects : Map[Int, (Int, Expression)],
                  symbols : Map[String, (Int, Expression)],
                  pc : List[Condition],
                  instructionHistory : List[Instruction] = Nil) {

}
