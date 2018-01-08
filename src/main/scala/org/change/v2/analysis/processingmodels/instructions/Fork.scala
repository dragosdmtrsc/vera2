package org.change.v2.analysis.processingmodels.instructions

import org.change.v2.analysis.memory.State
import org.change.v2.analysis.processingmodels.Instruction

/**
  * A small gift from radu to symnetic.
  */
case class Fork(forkBlocks: Iterable[Instruction]) extends Instruction {
  /**
    *
    * A state processing block produces a set of new states based on a previous one.
    *
    * @param s
    * @return
    */
  override def apply(s: State, verbose: Boolean): (List[State], List[State]) = {
    val (success, fail) = forkBlocks.map(i => i(s, verbose)).unzip
    (success.flatten.toList, fail.flatten.toList)
  }

  override def toString: String = {
    "Fork(\n" + forkBlocks.map(_.toString).mkString(";\n") + "\n)"
  }
}

object Fork {
  def apply(instrs: Instruction*): Instruction = Fork(instrs.toList)
}
