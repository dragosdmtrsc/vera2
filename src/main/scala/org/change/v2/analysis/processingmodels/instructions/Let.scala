package org.change.v2.analysis.processingmodels.instructions

import org.change.v2.analysis.memory.State
import org.change.v2.analysis.processingmodels.Instruction

case class Let(name : String, instruction: Instruction) extends Instruction {
  override def apply(s: State, verbose: Boolean): (List[State], List[State]) = {
    (s :: Nil, Nil)
  }
}
