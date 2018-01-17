package org.change.parser.p4.parser

import org.change.v2.analysis.expression.concrete.ConstantStringValue
import org.change.v2.analysis.processingmodels.Instruction
import org.change.v2.analysis.processingmodels.instructions._
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

  override lazy val parserCode: Instruction = StateExpander.parseStateMachine(expd, switch, codeFilter, name = switchInstance.getName + ".")

  override lazy val generatorCode : Instruction = StateExpander.generateAllPossiblePackets(expd, switch, name = switchInstance.getName + ".", codeFilter)

  override lazy val deparserCode : Instruction = StateExpander.deparserCode(expd, switch, codeFilter, name = switchInstance.getName + ".")

  protected lazy val extraCodeInternal: Map[String, Instruction] =
    StateExpander.deparserStateMachineToDict(expd, switch, codeFilter, name = switchInstance.getName + ".") ++
    StateExpander.stateMachineToDict(expd, switch, codeFilter, name = switchInstance.getName + ".") ++
    StateExpander.generateAllPossiblePacketsAsDict(expd, switch, codeFilter, name = switchInstance.getName + ".")

  override def extraCode() : Map[String, Instruction] = extraCodeInternal

  override def inlineGeneratorCode() =
    InstructionBlock(
      CreateTag("START", 0),
      Fork(
        expd.filter(s => codeFilter.getOrElse((_ : String) => true)(s.seflPortName)).
          map(x => extraCodeInternal(switchInstance.getName + "." + "generator." + x.seflPortName))
      )
    )
}

class SkipParserAndDeparser(switch : Switch, switchInstance: ISwitchInstance,
                            codeFilter : Option[Function1[String, Boolean]] = None)
  extends SwitchBasedParserGenerator(switch, switchInstance, codeFilter)
{

  override lazy val deparserCode : Instruction = Forward(s"${switchInstance.getName}.deparser.out")

  override val extraCode : Map[String, Instruction] = extraCodeInternal.
    filter(_._1.startsWith(s"${switchInstance.getName}.generator.")).
    map(h =>
      h._1 -> InstructionBlock(
        Assign("parser_fixpoint",
          ConstantStringValue(h._1.substring(s"${switchInstance.getName}.generator.".length))),
        h._2
      )) ++ extraCodeInternal.filter(_._1.startsWith(s"${switchInstance.getName}.parser.")).map(h => {
        h._1 -> If (Constrain("parser_fixpoint",
            :==:(ConstantStringValue(h._1.substring(s"${switchInstance.getName}.parser.".length)))),
          h._2,
          Fail(s"Wrong choice ${h._1}")
        )
      })
}

class TrivialDeparserGenerator (switch : Switch,
                        switchInstance: ISwitchInstance,
                        codeFilter : Option[Function1[String, Boolean]] = None
                       ) extends SwitchBasedParserGenerator(switch, switchInstance, codeFilter) {
  override lazy val deparserCode: Instruction = Forward(s"${switchInstance.getName}.deparser.out")
}
