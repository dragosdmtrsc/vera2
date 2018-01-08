package org.change.v2.analysis.processingmodels

import org.change.v2.analysis.memory.State


case class Unfail(instruction: Instruction) extends Instruction {
  override def apply(s: State, verbose: Boolean): (List[State], List[State]) = {
    instruction(s, verbose)
  }
}

case class SuperFork(instructions : List[Instruction]) extends Instruction {
  override def apply(s: State, verbose: Boolean): (List[State], List[State]) = {
    (Nil, Nil)
  }
}

case class Let(string : String, instruction: Instruction) extends Instruction {
  override def apply(s: State, verbose: Boolean): (List[State], List[State]) = {
    val applied = instruction(s)
    val fun = (x : State) => {
      x.copy(memory = x.memory.copy(
        symbols = x.memory.symbols.map( r => {
          string + "." + r._1 -> r._2
        }) ++ x.memory.rawObjects.map( r => {
          string + ".packet[" + r._1 + "]" -> r._2
        }),
        memTags = Map[String, Int]("START" -> 0)
      ))
    }
    (applied._1.map(fun), applied._2.map(fun))
  }
}