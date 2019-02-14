package org.change.v2.plugins.runner

import org.change.v2.analysis.constraint.{Condition, FALSE, FAND, TRUE}
import org.change.v2.analysis.executor.TripleInstructionExecutor
import org.change.v2.analysis.memory.{SimpleMemory, SimpleMemoryInterpreter, ToTheEndExecutor}
import org.change.v2.analysis.processingmodels.Instruction
import org.change.v2.plugins.eq.{ExecutorConsumer, ExecutorPlugin, PluginBuilder}

class DefaultExecutorImpl extends ExecutorPlugin {
  var totalSolverTime = 0l
  var nrSolverCalls = 0
  def simpleSatStrategy(condition: Condition,
                        newCondition: Condition): Boolean = {
    val start = java.lang.System.currentTimeMillis()
    val b = newCondition match {
      case TRUE => true
      case FALSE => false
      case _ =>
        SimpleMemory.isSatS(FAND.makeFAND(condition :: newCondition :: Nil))
    }
    val end = java.lang.System.currentTimeMillis()
    totalSolverTime += (end - start)
    nrSolverCalls += 1
    b
  }
  val simpleMemoryInterpreter = new SimpleMemoryInterpreter(simpleSatStrategy)
  override def apply(topology: collection.Map[String, Instruction],
                     startNodes: Set[String],
                     initials: List[SimpleMemory],
                     consumer: ExecutorConsumer): Unit = {
    val toTheEndExecutor = new ToTheEndExecutor(simpleMemoryInterpreter, topology.toMap)
    startNodes.foreach(x => {
      initials.foreach(u => {
        toTheEndExecutor.executeFromWithConsumer(x, u, consumer.apply)
      })
    })
  }
}

object DefaultExecutorImpl {
  class Builder extends PluginBuilder[DefaultExecutorImpl] {
    override def set(parm: String, value: String): PluginBuilder[DefaultExecutorImpl] = this
    override def build(): DefaultExecutorImpl = new DefaultExecutorImpl
  }
}
