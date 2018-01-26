package org.change.parser

import java.util.UUID

import org.change.v2.analysis.memory.State
import org.change.v2.analysis.processingmodels.Instruction
import org.change.v2.analysis.processingmodels.instructions.{Forward, InstructionBlock}

package object p4 {
  def anonymizeAndForward(port : String, except : Set[String] = Set.empty): Instruction = {
    InstructionBlock(
      Instruction(anonymize(_, except)),
      Forward(port)
    )
  }

  def anonymize(state: State, except : Set[String] = Set.empty): (List[State], List[State]) = {
    (state.copy(memory = state.memory.copy(symbols = state.memory.symbols.filter(r => ! (except contains r._1)).
      map(r => s"${r._1}_anon${UUID.randomUUID().toString}" -> r._2) ++ state.memory.symbols.filter(except contains _._1))) :: Nil,
      Nil : List[State])
  }

}
