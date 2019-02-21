package org.change.plugins.runner

import org.change.v2.analysis.constraint.{Condition, FALSE, FAND, TRUE}
import org.change.v2.analysis.executor.TripleInstructionExecutor
import org.change.v2.analysis.memory.{SimpleMemory, SimpleMemoryInterpreter, ToTheEndExecutor}
import org.change.v2.analysis.processingmodels.Instruction
import org.change.v2.analysis.processingmodels.instructions.{Fork, Forward}
import org.change.plugins.eq.{ExecutorConsumer, ExecutorPlugin, PluginBuilder}

import scala.util.matching.Regex

class DefaultExecutorImpl extends ExecutorPlugin {
  var totalSolverTime = 0l
  var nrSolverCalls = 0
  var mergePoints: Regex = ("(.*\\.parser\\.out)|(.*\\.parser$)|" +
    "(.*\\.control\\.ingress\\.out)|(.*\\.control\\.egress\\.out)|(.*\\.parser\\..*\\.escape)").r

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
    val toTheEndExecutor = new ToTheEndExecutor(simpleMemoryInterpreter, topology.toMap, (s : String) => {
      mergePoints.findFirstMatchIn(s).nonEmpty
    })
    initials.foreach(u => {
      toTheEndExecutor.executeFromWithConsumer(startNodes, u, consumer.apply _, None)
    })
    consumer.flush()
  }
}

object DefaultExecutorImpl {
  class Builder extends PluginBuilder[DefaultExecutorImpl] {
    override def set(parm: String, value: String): PluginBuilder[DefaultExecutorImpl] = this
    override def build(): DefaultExecutorImpl = new DefaultExecutorImpl
  }
}
