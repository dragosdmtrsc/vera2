package org.change.parser.p4

import java.util
import java.util.UUID

import org.change.parser.p4.buffer.{BufferMechanism, Deparser, DeparserRev, OutputMechanism}
import org.change.parser.p4.parser.{DFSState, StateExpander}
import org.change.utils.abstractions._
import org.change.v2.analysis.executor.{Executor, InstructionExecutor}
import org.change.v2.analysis.memory.State
import org.change.v2.analysis.processingmodels._
import org.change.v2.analysis.processingmodels.instructions._
import org.change.v2.executor.clickabstractnetwork.executionlogging.{ExecutionLogger, NoLogging}
import org.change.v2.p4.model.SwitchInstance
import org.change.v2.p4.model.parser.ReturnStatement

import scala.collection.JavaConversions._
import scala.util.matching.Regex
/**
  * Created by dragos on 07.09.2017.
  */
class ControlFlowInterpreter(val switchInstance: SwitchInstance) {
  val tables = switchInstance.getDeclaredTables.map(new FullTable(_, switchInstance))
  val switch = switchInstance.getSwitchSpec

  private val initializeCode = new InitializeCode(switchInstance)
  private lazy val expd = new StateExpander(switch, "start").doDFS(DFSState(0))

  for (l <- switch.getCtx.links) {
    if (l._1.contains("table.")) {
      val tableExactMatcher = "table\\.(.*?)\\.out\\.(.*)".r
      tableExactMatcher.findAllMatchIn(l._1).foreach(x => {
        val tabName = x.group(1)
        val id = x.group(2)
        switch.getCtx.instructions.put(s"table.$tabName.in.$id", new FullTable(tabName, switchInstance, id).fullAction())
      })
    }
  }


  // plug in the buffer mechanism
  private lazy val bufferMechanism = new BufferMechanism(switchInstance)
  switch.getCtx.instructions.put(s"${switchInstance.getName}.buffer.in", bufferMechanism.symnetCode())
  switch.getCtx.links.put(bufferMechanism.outName(), "control.egress")

  // plug in the output mechanism
  private lazy val outputMechanism = new OutputMechanism(switchInstance)
  switch.getCtx.instructions.put(s"${switchInstance.getName}.output.in", outputMechanism.symnetCode())
  //plug egress.out -> <sw>.deparser
  switch.getCtx.links.put("control.egress.out", s"${switchInstance.getName}.deparser.in")

  private lazy val deparser = new DeparserRev(switchInstance)
  // plug in the deparser
  switch.getCtx.instructions.put(s"${switchInstance.getName}.deparser.in", deparser.symnetCode())
  // link deparser -> <sw>.output.in
  switch.getCtx.links.put(deparser.outName(), s"${switchInstance.getName}.output.in")

  //plug in the parser
  switch.getCtx.instructions.put(s"${switchInstance.getName}.parser", parserCode())
  // plug in the switch instances
  for (x <- switchInstance.getIfaceSpec.keySet()) {
    switch.getCtx.instructions.put(s"${switchInstance.getName}.input.$x", initialize(x))
    // connect <sw>.input.x.out -> <sw>.parser
    switch.getCtx.links.put(s"${switchInstance.getName}.input.$x.out", s"${switchInstance.getName}.parser")
  }

  // plug control.ingress.out -> <sw>.buffer
  switch.getCtx.links.put(s"control.ingress.out", s"${switchInstance.getName}.buffer.in")


  private val instructionsCached = switch.getCtx.instructions.toMap
  private val linksCached = switch.getCtx.links.toMap
  def instructions() : Map[String, Instruction] = {
    instructionsCached
  }
  def links() : Map[String, String] = {
    linksCached
  }

  def initialize(port : Int) = initializeCode.switchInitializePacketEnter(port)
  def initializeGlobally() = initializeCode.switchInitializeGlobally()

  def allParserStatesInstruction() = StateExpander.generateAllPossiblePackets(expd, switch)
  def parserCode() = StateExpander.parseStateMachine(expd, switch)


  private def normalize(str : String) = str.replace(".", "_").
    replace("[", "_").
    replace("]", "_").
    replace("-", "_")
  def toDot() : String = {
    val sb = new StringBuilder()
    sb.append(s"digraph ${switchInstance.getName} {\n")

    for (x <- instructions()) {
      sb.append("    " + normalize(x._1) + ";\n")
      def parseInstrGraph(parent : String, i : Instruction) : Unit = {
        i match {
          case ib : InstructionBlock => for (c <- ib.instructions) parseInstrGraph(parent, c)
          case fw : Forward => sb.append(s"${normalize(parent)} -> ${normalize(fw.place)};\n")
          case frk : Fork => {
            val crt = s"Fork_${UUID.randomUUID().toString}"
//            sb.append(normalize(crt) + ";\n")
//            sb.append(s"${normalize(parent)} -> ${normalize(crt)};\n")
            for (fb <- frk.forkBlocks)
              parseInstrGraph(parent, fb)
          }
          case iif : If => {
            val crt = s"If_${UUID.randomUUID().toString}"
//            sb.append(normalize(crt) + ";\n")
//            sb.append(s"${normalize(parent)} -> ${normalize(crt)};\n")
            parseInstrGraph(parent, iif.thenWhat)
            parseInstrGraph(parent, iif.elseWhat)
          }
//          case fl : Fail => sb.append(s"${normalize(parent)} -> Fail_${normalize(UUID.randomUUID().toString)};\n")
          case _ => Unit
        }
      }
      parseInstrGraph(x._1, x._2)
    }
    for (x <- links()) {
      sb.append("    " + normalize(x._1) + ";\n")
      sb.append("    " + normalize(x._2) + ";\n")
      sb.append("    " + normalize(x._1) + " -> " + normalize(x._2) + ";\n")
    }

    sb.append("}")
    sb.toString()
  }

}

case class P4ExecutionContext(instructions: Map[LocationId, Instruction],
  links: Map[LocationId, LocationId],
  okStates: List[State],
  executor: InstructionExecutor,
  failedStates: List[State] = Nil,
  stuckStates: List[State] = Nil) {

  /**
    * Is there any state further explorable ?
    * @return
    */
  def isDone: Boolean = okStates.isEmpty

  /**
    * Calls execute until nothing can be explored further more. (The result is a done Execution Context)
    * @param verbose
    * @return
    */
  def untilDone(verbose: Boolean): P4ExecutionContext = if (isDone) {
    //    ClickExecutionContext.executorService.shutdownNow()
    this
  } else this.execute(verbose).untilDone(verbose)

  private def navigate(s : State) = {
    var current = s
    while (links contains current.location) {
      current = current.forwardTo(links(current.location))
    }
    current
  }

  def execute(verbose: Boolean = false): P4ExecutionContext = {
    val (ok, fail, stuck) = (for {
      sPrime <- okStates
      s = navigate(sPrime)
      stateLocation = s.location
    } yield {
      if (instructions contains stateLocation) {
        //          Apply instructions
        val i = instructions(stateLocation)
        val r1 = executor.execute(i, s, verbose)
        //          Apply check instructions on output ports

        (r1._1, r1._2, Nil)
      } else
        (Nil, Nil, List(s))
    }).unzip3

    copy(
      okStates = ok.flatten,
      failedStates = failedStates ++ fail.flatten,
      stuckStates = stuckStates ++ stuck.flatten
    )
  }
}


object ControlFlowInterpreter {
  def apply(switchInstance: SwitchInstance): ControlFlowInterpreter = new ControlFlowInterpreter(switchInstance)

  def apply(p4File: String, dataplane: String, ifaces: Map[Int, String], name : String = "") : ControlFlowInterpreter =
    ControlFlowInterpreter(SwitchInstance.fromP4AndDataplane(p4File, dataplane, name, ifaces.foldLeft(new util.HashMap[Integer, String]())((acc, x) => {
      acc.put(x._1, x._2)
      acc
    })))

}
