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
import org.change.v2.p4.model.actions.primitives.NoOp
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

  def toVis() : String = {
    val sb = new StringBuilder()
    sb.append("<!doctype html>\n<html>\n<head>\n  <title>Network | Basic usage</title>\n\n  <script type=\"text/javascript\" src=\"../../Vis.js/dist/vis.js\"></script>\n  <link href=\"../../Vis.js/dist/vis-network.min.css\" rel=\"stylesheet\" type=\"text/css\" />\n\n  <style type=\"text/css\">\n    #mynetwork {\n      width: 1500px;\n      height: 1000px;\n      border: 1px solid lightgray;\n    }\n  </style>\n</head>\n<body>")
    sb.append("<div id=\"mynetwork\"></div>\n\n<script type=\"text/javascript\">\n")

    val blockId = new scala.collection.mutable.HashMap[String,Int]();
    var crtID = 1;

    for (x <- links()) {
      if (!blockId.contains(x._1)) {
        blockId(x._1) = crtID;
        crtID = crtID+1;
      }
      if (!blockId.contains(x._2)) {
        blockId(x._2) = crtID;
        crtID = crtID+1;
      }
    }
    sb.append("  var edges = new vis.DataSet([\n")

    var first = true;

    for (x <- links()) {
      if (!first)
        sb.append(",\n");

      first = false;

      sb.append(s"{from: ${blockId(x._1)}, to: ${blockId(x._2)}, arrows:'to'}");
    }

    for (x <- instructions()) {
      if (!blockId.contains(normalize(x._1))){
        blockId(x._1) = crtID
        crtID = crtID + 1
      }

      def parseInstrGraph(parent : String, i : Instruction) : Unit = {
        i match {
          case ib : InstructionBlock => for (c <- ib.instructions) parseInstrGraph(parent, c)
          case fw : Forward =>
            if (!first)
              sb.append(",\n")
            first = false

            if (!blockId.contains(normalize(parent))){
              blockId(parent) = crtID
              crtID = crtID + 1
            }
            if (!blockId.contains(fw.place)){
              blockId(fw.place) = crtID
              crtID = crtID + 1
            }

            sb.append(s"{ from: ${blockId(parent)}, to: ${blockId(fw.place)}, arrows:'to' }")

          case frk : Fork => {
            for (fb <- frk.forkBlocks)
              parseInstrGraph(parent, fb)
          }
          case iif : If => {
            parseInstrGraph(parent, iif.thenWhat)
            parseInstrGraph(parent, iif.elseWhat)
          }
          //          case fl : Fail => sb.append(s"${normalize(parent)} -> Fail_${normalize(UUID.randomUUID().toString)};\n")
          case _ => Unit
        }
      }
      parseInstrGraph(x._1, x._2)
    }
    sb.append("  ]);\n\n")

    def genInstrTitle(i : Instruction) : String = {
      val sb2 = new StringBuilder()

      i match {
        case ib : InstructionBlock =>
          if (ib.instructions.size > 1) {
            sb2.append("{<br>")
            for (c <- ib.instructions) {
              sb2.append(s"${genInstrTitle(c)}<br>");
            }
            sb2.append("}<br>")
          }
          else if (ib.instructions.size==1)
            genInstrTitle(ib.instructions.head)

        case fw : Forward =>
          sb2.append(s"FORWARD ${fw.place}")

        case frk : Fork => {
          sb2.append("Fork {<br>")
          for (fb <- frk.forkBlocks)
            sb2.append(s"| ${genInstrTitle(fb)}<br>")
          sb2.append("}<br>")
        }
        case iif : If => {
          sb2.append(s"if ( ${genInstrTitle(iif.testInstr)} ) then ${genInstrTitle(iif.thenWhat)} <br> else ${genInstrTitle(iif.elseWhat)} <br>");
        }
        //          case fl : Fail => sb.append(s"${normalize(parent)} -> Fail_${normalize(UUID.randomUUID().toString)};\n")
        case c : ConstrainNamedSymbol =>
          sb2.append(s"ConstrainNamedSymbol (${c.id} ${c.dc})")

        case d : ConstrainRaw =>
          sb2.append(s"ConstrainRaw (${d.a} ${d.dc})")

        case d : AllocateSymbol =>
          sb2.append(s"AllocateSymbol(${d.id}, size ${d.size})")

        case d : AllocateRaw =>
          sb2.append(s"AllocateRaw(at ${d.a}, size ${d.size})")

        case d : DeallocateNamedSymbol =>
          sb2.append(s"DellocateNamedSymbol(${d.id})")

        case d : DeallocateRaw =>
          sb2.append(s"DeallocateRaw(at ${d.a}, size ${d.size})")

        case d : CreateTag =>
          sb2.append(s"CreateTag(${d.name},${d.value})")

        case d : DestroyTag =>
          sb2.append(s"DestroyTag(${d.name})")

        case d : Check =>
          sb2.append(s"Check(at ${d.offset}, size ${d.size})")

        case d : AssignRaw =>
          sb2.append(s"Header(${d.a}) = ${d.exp}")

        case d : AssignNamedSymbol =>
          sb2.append(s"${d.id} = ${d.exp}")

        case d: Fail =>
          sb2.append(s"Fail<")

        case _ => sb2.append(s"GenericInstruction")
      }
      sb2.toString()
    }

    sb.append("  var nodes = new vis.DataSet([\n")
    first = true;
    for ((k,v)<- blockId) {
      if (!first)
        sb.append(",\n");

      first = false;
      if (instructions().contains(k))
        sb.append(s"{id: ${v}, label: '${normalize(k)}', title: '${genInstrTitle(instructions()(k))}'}");
      else
        sb.append(s"{id: ${v}, label: '${normalize(k)}', title: 'NULL'}");
    }
    sb.append("\n]);\n\n");

    sb.append("// create a network\n  var container = document.getElementById('mynetwork');\n  var data = {\n    nodes: nodes,\n    edges: edges\n  };\n  var options = {};\n  var network = new vis.Network(container, data, options);\n</script>\n\n\n</body>\n</html>")
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
