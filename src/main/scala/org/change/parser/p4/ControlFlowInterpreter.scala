package org.change.parser.p4

import java.util

import org.change.parser.p4.buffer.{BufferMechanism, OutputMechanism}
import org.change.parser.p4.factories.{FullTableFactory, GlobalInitFactory, InitCodeFactory, InstanceBasedInitFactory}
import org.change.parser.p4.parser.{DFSState, ParserGenerator, StateExpander, SwitchBasedParserGenerator}
import org.change.parser.p4.tables.FullTable
import org.change.v2.analysis.executor.InstructionExecutor
import org.change.v2.analysis.memory.State
import org.change.v2.analysis.processingmodels._
import org.change.v2.analysis.processingmodels.instructions._
import org.change.v2.p4.model.{ISwitchInstance, Switch, SwitchInstance}
import org.change.v2.util.Rewriter

import scala.collection.JavaConversions._
import org.change.v2.util.Rewriter._

/**
  * Created by dragos on 07.09.2017.
  */
class ControlFlowInterpreter[T<:ISwitchInstance](val switchInstance: T,
                                                 val switch: Switch,
                                                 val additionalInitCode : (T, Int) => Instruction,
                                                 val tableFactory : (T, String, String) => Instruction,
                                                 val initFactory : (T) => Instruction,
                                                 val parserGenerator: ParserGenerator) {
  def this(switchInstance: T, switch: Switch,
           optAdditionalInitCode : Option[(T, Int) => Instruction] = None,
           optTableFactory : Option[(T, String, String) => Instruction] = None,
           optInitFactory : Option[(T) => Instruction] = None,
           optParserGenerator : Option[ParserGenerator] = None) = this(
    switchInstance, switch,
    optAdditionalInitCode.getOrElse(InitCodeFactory.get(switchInstance.getClass.asInstanceOf[Class[T]])),
    optTableFactory.getOrElse(FullTableFactory.get(switchInstance.getClass.asInstanceOf[Class[T]])),
    optInitFactory.getOrElse(GlobalInitFactory.get(switchInstance.getClass.asInstanceOf[Class[T]])),
    optParserGenerator.getOrElse(new SwitchBasedParserGenerator(switch, switchInstance))
  )
  def this(switchInstance: T, switch: Switch) = this(switchInstance, switch, None, None, None, None)
  private def renamer(s : String) = if (s.startsWith(s"${switchInstance.getName}."))
    s
  else {
    s"${switchInstance.getName}.$s"
  }

  private val initializeCode = new InitializeCode(switchInstance, switch, additionalInitCode, initFactory)

  private val controlFlowInstructions =
    Rewriter.rewrite(switch.getControlFlowInstructions.toMap).transform()(renamer)._1
  private val controlFlowLinks = Rewriter.linkRewriter(switch.getControlFlowLinks.toMap).transform()(renamer)._2
  private val controlExactMatcher = "control\\.(.*?)\\.out\\.(.*)".r

  private val tableExactMatcher = "table\\.(.*?)\\.out\\.(.*)".r
  val plugTables: Map[String, Instruction] = controlFlowLinks.
    filter(r => tableExactMatcher.findFirstMatchIn(r._1).isDefined).flatMap(r => {
      tableExactMatcher.findFirstMatchIn(r._1).map(x => {
        val tabName = x.group(1)
        val id = x.group(2)
        s"${switchInstance.getName}.table.$tabName.in.$id" -> {
          tableFactory(switchInstance, tabName, id)
        }
      })
    })
  println("tables parsed OK")
  // plug in the buffer mechanism
  private val bufferMechanism = new BufferMechanism(switchInstance)
  private val bufferPlug = Map[String, Instruction](s"${switchInstance.getName}.buffer.in" -> bufferMechanism.symnetCode())
  private val bufferOutLink = Map[String, String](bufferMechanism.outName() -> "control.egress")

  // plug in the output mechanism
  private lazy val outputMechanism = new OutputMechanism(switchInstance)
  //plug egress.out -> <sw>.deparser
  private val outputPlug = Map[String, Instruction](s"${switchInstance.getName}.output.in" -> outputMechanism.symnetCode())
  private val egressOutLink = Map[String, String](s"${switchInstance.getName}.control.egress.out" -> s"${switchInstance.getName}.deparser.in")

  // plug in the deparser
  private val deparserPlug = rewrite(
    Map[String, Instruction](s"${switchInstance.getName}.deparser.in" -> parserGenerator.deparserCode())
  ).transform()(renamer)._1
  // link deparser -> <sw>.output.in
  private val deparserOutLink = Map[String, String](s"${switchInstance.getName}.deparser.out" -> s"${switchInstance.getName}.output.in")
  //plug in the parser
  def parserCode(): Instruction = parserGenerator.parserCode()
  private val parserPlug =  Rewriter.
    rewrite(Map[String, Instruction](s"${switchInstance.getName}.parser" -> parserCode())).transform()(renamer)._1
  // plug in the switch instances
  private val ifacePlug = switchInstance.getIfaceSpec.keys.flatMap(x => {
    Map[String, Instruction](s"${switchInstance.getName}.input.$x" -> initialize(x))
  })

  private val inputOutLink = switchInstance.getIfaceSpec.keys.flatMap(x => {
    Map[String, String](s"${switchInstance.getName}.input.$x.out" -> s"${switchInstance.getName}.parser")
  })


  // plug control.ingress.out -> <sw>.buffer
  private val ingressOutLink = Map[String, String](s"${switchInstance.getName}.control.ingress.out" -> s"${switchInstance.getName}.buffer.in")
  private val (instructionsCached, linksCached) =
    (controlFlowInstructions ++ parserPlug ++ outputPlug ++ deparserPlug ++ ifacePlug ++ plugTables ++ bufferPlug ++
      Rewriter.rewrite(parserGenerator.extraCode()).transform()(renamer)._1,
    controlFlowLinks ++ deparserOutLink ++ inputOutLink ++ ingressOutLink ++ bufferOutLink ++ egressOutLink)

  def instructions() : Map[String, Instruction] = instructionsCached
  def links() : Map[String, String] = linksCached

  def startWherever() : Instruction = Fork(switchInstance.getIfaceSpec.keys.map(x => {
      Forward(s"${switchInstance.getName}.input.$x")
    }))


  def initialize(port : Int): Instruction = initializeCode.switchInitializePacketEnter(port)
  def initializeGlobally(): Instruction = initializeCode.switchInitializeGlobally()

  def allParserStatesInstruction(): Instruction = parserGenerator.generatorCode()

  def allParserStatesInline() : Instruction = parserGenerator.inlineGeneratorCode()

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

  GlobalInitFactory.register(classOf[SwitchInstance], (r : ISwitchInstance) => {
    new InstanceBasedInitFactory(r.asInstanceOf[SwitchInstance]).initCode()
  })
  FullTableFactory.register(classof = classOf[SwitchInstance], (switchInstance : ISwitchInstance, tabName : String, id : String) => {
    new FullTable(tabName, switchInstance.asInstanceOf[SwitchInstance], id).fullAction()
  })

  def apply(switchInstance: SwitchInstance,
            optAdditionalInitCode : Option[(SwitchInstance, Int) => Instruction],
            optTableFactory : Option[(SwitchInstance, String, String) => Instruction],
            optInitFactory : Option[(SwitchInstance) => Instruction],
            optGenerator : Option[ParserGenerator]): ControlFlowInterpreter[SwitchInstance] =
    new ControlFlowInterpreter(switchInstance,
      switchInstance.getSwitchSpec,
      optAdditionalInitCode,
      optTableFactory,
      optInitFactory,
      optGenerator)

  def apply(p4File: String, dataplane: String, ifaces: Map[Int, String], name : String = "",
            optAdditionalInitCode : Option[(SwitchInstance, Int) => Instruction] = None,
            optTableFactory : Option[(SwitchInstance, String, String) => Instruction] = None,
            optInitFactory : Option[(SwitchInstance) => Instruction] = None,
            optGenerator : Option[ParserGenerator] = None): ControlFlowInterpreter[SwitchInstance] =
    ControlFlowInterpreter(SwitchInstance.fromP4AndDataplane(p4File, dataplane, name, ifaces.foldLeft(new util.HashMap[Integer, String]())((acc, x) => {
      acc.put(x._1, x._2)
      acc
    })), optAdditionalInitCode,
      optTableFactory,
      optInitFactory,
      optGenerator)

}
