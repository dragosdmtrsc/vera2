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
  override def apply(topology: collection.Map[String, i2],
                     startNodes: Set[String],
                     initials: List[sm2],
                     consumer: ExecutorConsumer): Unit = {
    val toTheEndExecutor = new InterExecutor(V2Translator.v2v3(topology.toMap), simpleMemoryInterpreter,
      (s : String) => { mergePoints.map(_.findFirstMatchIn(s).nonEmpty).getOrElse(false) })
    val actualConsumer = (V2Translator.mem3mem2 _).andThen(consumer.apply)
    val start = System.currentTimeMillis()
    initials.map(V2Translator.mem2mem3).foreach(u => {
      toTheEndExecutor.executeFromWithConsumer(startNodes, u, actualConsumer)
    })
    val end = System.currentTimeMillis()
    Logger.getLogger(this.getClass.getName).info(s"done in ${end - start}ms")
    consumer.flush()
  }
}
object DefaultExecutorImplv3 {
  class Builder extends PluginBuilder[DefaultExecutorImplv3] {
    override def set(parm: String, value: String): PluginBuilder[DefaultExecutorImplv3] = this
    override def build(): DefaultExecutorImplv3 = new DefaultExecutorImplv3
  }
}

