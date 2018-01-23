package org.change.parser

import java.util.UUID

import org.change.v2.analysis.memory.State
import org.change.v2.analysis.processingmodels.Instruction
import org.change.v2.analysis.processingmodels.instructions.{Forward, InstructionBlock}

package object p4 {
  def anonymizeAndForward(port : String): Instruction = {
    InstructionBlock(
      Instruction(anonymize),
      Forward(port)
    )
  }

  def anonymize(state: State): (List[State], List[State]) = {
    (state.copy(memory = state.memory.copy(symbols = state.memory.symbols.map(r => s"${r._1}_anon${UUID.randomUUID().toString}" -> r._2))) :: Nil,
      Nil : List[State])
  }

}
