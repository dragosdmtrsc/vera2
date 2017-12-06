package org.change.v2.runners.experiments

import org.change.v2.analysis.executor.InstructionExecutor
import org.change.v2.analysis.expression.concrete.SymbolicValue
import org.change.v2.analysis.memory.State
import org.change.v2.analysis.processingmodels.instructions.{Assign, Fail, Forward, InstructionBlock}
import org.change.v2.executor.clickabstractnetwork.ClickExecutionContext
import org.change.v2.executor.clickabstractnetwork.executionlogging.JsonLogger

/**
  * A small gift from radu to symnetic.
  */
object SimpleExecutor {

  def main(args: Array[String]): Unit = {
    val exe = ClickExecutionContext(
      instructions = Map("0" -> InstructionBlock(
        Assign("a", SymbolicValue()),
        Forward("b"))
        ,
        "b" -> Fail())
      ,
      links = Map("a" -> "b"),
      okStates = List(State.clean.forwardTo("a")),
      logger = JsonLogger, executor = InstructionExecutor()
    )

    val done = exe.untilDone(true)
    println(done)


  }

}
