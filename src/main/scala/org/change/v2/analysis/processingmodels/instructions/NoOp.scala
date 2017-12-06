package org.change.v2.analysis.processingmodels.instructions

import org.change.v2.analysis.memory.State
import org.change.v2.analysis.processingmodels.Instruction


object NoOp extends Instruction {
  override def apply(s: State, v: Boolean): (List[State], List[State]) =
    (List((if (v) s.addInstructionToHistory(this) else s)), Nil)

  override def toString = {
    "NoOp"
  }
}
