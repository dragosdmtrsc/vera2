package org.change.v2.analysis.processingmodels.instructions

import org.change.v2.analysis.processingmodels.Instruction
import org.change.v2.analysis.processingmodels.State



object NoOp extends Instruction {
  override def apply(s: State, v: Boolean): (List[State], List[State]) =
    (List((if (v) s.addInstructionToHistory(this) else s)), Nil)
    
  override def toString = {
    "NoOp"
  }
}
