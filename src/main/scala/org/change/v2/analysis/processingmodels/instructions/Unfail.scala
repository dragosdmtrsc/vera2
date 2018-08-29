package org.change.v2.analysis.processingmodels

import org.change.v2.analysis.memory.State


case class Unfail(instruction: Instruction) extends Instruction {
  override def apply(s: State, verbose: Boolean): (List[State], List[State]) = {
    instruction(s, verbose)
  }
}

case class SuperFork(instructions : Iterable[Instruction]) extends Instruction {
  override def apply(s: State, verbose: Boolean): (List[State], List[State]) = {
    (Nil, Nil)
  }
}