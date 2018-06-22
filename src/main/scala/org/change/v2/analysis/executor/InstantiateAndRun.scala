package org.change.v2.analysis.executor

import org.change.v2.analysis.executor.solvers.AlwaysTrue
import org.change.v2.analysis.expression.concrete.SymbolicValue
import org.change.v2.analysis.memory.State
import org.change.v2.analysis.processingmodels.Instruction
import org.change.v2.analysis.processingmodels.instructions.{Allocate, Assign, InstructionBlock, NoOp}

object InstantiateAndRun {

  def apply(instruction : Instruction,
            symbols : Iterable[(String, Int)],
            alsoAdd : Instruction = NoOp,
            state : State = State.clean,
            instructionExecutor: TripleInstructionExecutor = new TripleInstructionExecutor(AlwaysTrue)):
    (List[State], List[State], List[State]) = {
    val ib = InstructionBlock(symbols.toList.flatMap(r => {
        val sz = r._2
        List(
          Allocate(r._1, sz),
          Assign(r._1, SymbolicValue(r._1))
        )
      }) :+ alsoAdd
    )
    val (s1, s2, s3) = instructionExecutor.execute(ib, state, verbose = false)
    s3.foldLeft((s1, s2, List.empty[State]))((acc, x) => {
      val (n1, n2, n3) = instructionExecutor.execute(instruction, x, verbose = true)
      (acc._1 ++ n1, acc._2 ++ n2, acc._3 ++ n3)
    })
  }
}
