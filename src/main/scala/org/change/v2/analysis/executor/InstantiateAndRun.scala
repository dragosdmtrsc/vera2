package org.change.v2.analysis.executor

import org.change.v2.analysis.executor.solvers.AlwaysTrue
import org.change.v2.analysis.expression.concrete.SymbolicValue
import org.change.v2.analysis.memory.State
import org.change.v2.analysis.processingmodels.Instruction
import org.change.v2.analysis.processingmodels.instructions.{Allocate, Assign, InstructionBlock}

object InstantiateAndRun {

  def apply(instruction : Instruction,
            symbols : Iterable[(String, Int)],
            state : State = State.clean,
            instructionExecutor: TripleInstructionExecutor = new TripleInstructionExecutor(AlwaysTrue)):
    (List[State], List[State], List[State]) = {
    val ib = InstructionBlock(symbols.toList.flatMap(r => {
        val sz = r._2
        List(
          Allocate(r._1, sz),
          Assign(r._1, SymbolicValue(r._1))
        )
      }) :+ instruction
    )
    instructionExecutor.execute(ib, state, verbose = true)
  }
}
