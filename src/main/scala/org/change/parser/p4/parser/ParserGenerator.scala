package org.change.parser.p4.parser

import org.change.v2.analysis.processingmodels.Instruction
import org.change.v2.analysis.processingmodels.instructions.{Call, CreateTag, Fork, InstructionBlock}
import org.change.v2.p4.model.{ISwitchInstance, Switch}
import org.change.v2.analysis.memory.TagExp.IntImprovements

trait ParserGenerator {
  def parserCode() : Instruction
  def generatorCode() : Instruction
  def inlineGeneratorCode() : Instruction
  def deparserCode() : Instruction
  def extraCode() : Map[String, Instruction]
}


class SwitchBasedParserGenerator(switch : Switch,
                                 switchInstance: ISwitchInstance,
                                 codeFilter : Option[Function1[String, Boolean]] = None
                                ) extends ParserGenerator {
  private lazy val expd = new StateExpander(switch, "start").doDFS(DFSState(0))

  override lazy val parserCode: Instruction = StateExpander.parseStateMachine(expd, switch, codeFilter)

  override lazy val generatorCode : Instruction = StateExpander.generateAllPossiblePackets(expd, switch, switchInstance.getName, codeFilter)

  override lazy val deparserCode : Instruction = StateExpander.deparserCode(expd, switch, codeFilter)

  override lazy val extraCode : Map[String, Instruction] =
      StateExpander.deparserStateMachineToDict(expd, switch, codeFilter) ++
      StateExpander.stateMachineToDict(expd, switch, codeFilter) ++
      StateExpander.generateAllPossiblePacketsAsDict(expd, switch, codeFilter)

  override def inlineGeneratorCode() =
    InstructionBlock(
      CreateTag("START", 0),
      Fork(
        expd.filter(s => codeFilter.getOrElse((_ : String) => true)(s.seflPortName)).
          map(x => extraCode("generator." + x.seflPortName))
      )
    )
}
