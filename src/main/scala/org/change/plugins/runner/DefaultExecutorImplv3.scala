package org.change.plugins.runner

import java.util.logging.Logger

import org.change.plugins.eq.{ExecutorConsumer, ExecutorPlugin, PluginBuilder}
import org.change.v2tov3.V2Translator
import org.change.v3.semantics._
import org.change.v3.syntax.Instruction
import org.change.v2.analysis.processingmodels.{Instruction => i2}
import org.change.v2.analysis.memory.{SimpleMemory => sm2}
import z3.scala.{Z3AST, Z3AppAST}

import scala.util.matching.Regex

class DefaultExecutorImplv3 extends ExecutorPlugin {
  var totalSolverTime = 0l
  var nrSolverCalls = 0
  var mergePoints: Option[Regex] = Some(("(.*\\.parser\\.out)|(.*\\.parser$)|" +
    "(.*\\.control\\.ingress$)|(table\\.(.*?)\\.out\\.(.*))|" +
    "(.*\\.control\\.ingress\\.out)|(.*\\.control\\.egress\\.out)|(.*\\.parser\\..*\\.escape)").r)

  override def supports(): Set[Int] = Set(2, 3)
  def simpleSatStrategy(condition: SimplePathCondition,
                        newCondition: Condition): Option[SimplePathCondition] = {
    val start = java.lang.System.currentTimeMillis()
    val simpd = context.simplifyAst(newCondition)
    if (simpd == context.mkFalse())
      None
    else if (simpd == context.mkTrue())
      Some(condition)
    else {
      val slv = context.mkSolver()
      for (x <- condition)
        slv.assertCnstr(x)
      slv.assertCnstr(simpd)
      val b = slv.check().get
      slv.decRef()
      val end = java.lang.System.currentTimeMillis()
      totalSolverTime += (end - start)
      nrSolverCalls += 1
      if (b) Some(newCondition :: condition)
      else None
    }
  }
  val simpleMemoryInterpreter = new IntraExecutor
  override def apply(topology: scala.collection.Map[String, i2],
                     startNodes: Set[String],
                     initials: List[sm2],
                     consumer: ExecutorConsumer): Unit = {
    assert(consumer.supports().contains(2))
    val topo3 = V2Translator.v2v3(topology)
    val initials3 = initials.map(V2Translator.mem2mem3)
    val actualConsumer3 = (V2Translator.mem3mem2 _).andThen(consumer.apply)
    run3(topo3, startNodes, initials3, actualConsumer3)
    consumer.flush()
  }

  private def run3(topo3: collection.Map[String, Instruction],
                   startNodes: Set[String],
                   initials3: List[SimpleMemory],
                   actualConsumer3: SimpleMemory => Unit): Unit = {
    val toTheEndExecutor = new InterExecutor(topo3, simpleMemoryInterpreter,
      (s : String) => { mergePoints.exists(_.findFirstMatchIn(s).nonEmpty) })

    val start = System.currentTimeMillis()
    initials3.foreach(u => {
      toTheEndExecutor.executeFromWithConsumer(startNodes, u, actualConsumer3)
    })
    val end = System.currentTimeMillis()
    Logger.getLogger(this.getClass.getName).info(s"done in ${end - start}ms")
  }
  override def apply3(topo3: collection.Map[String, Instruction],
                      startNodes: Set[String],
                      initials3: List[SimpleMemory],
                      consumer: ExecutorConsumer): Unit = {
    assert(consumer.supports().contains(3))
    run3(topo3, startNodes, initials3, x => consumer.apply3(x))
    consumer.flush()
  }
}
object DefaultExecutorImplv3 {
  class Builder extends PluginBuilder[DefaultExecutorImplv3] {
    override def set(parm: String, value: String): PluginBuilder[DefaultExecutorImplv3] = this
    override def build(): DefaultExecutorImplv3 = new DefaultExecutorImplv3
  }
}

