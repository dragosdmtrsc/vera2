package org.change.v2.analysis.processingmodels.instructions

import org.change.v2.analysis.memory.State
import org.change.v2.analysis.processingmodels.Instruction

/**
  * A small gift from radu to symnetic.
  */
case class Check(offset: Int, size: Int) extends Instruction {
  override def apply(s: State, verbose: Boolean): (List[State], List[State]) =
    optionToStatePair(if (verbose) s.addInstructionToHistory(this) else s, s"Check failed at $offset size $size") (s => {
      if (s.memory.doesNotOverlap(offset, size)) Some(s.memory) else None
    })
}
