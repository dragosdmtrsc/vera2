package org.change.parser.p4.parser

import org.change.v2.analysis.memory.State
import org.change.v2.analysis.processingmodels.Instruction
import org.change.v2.p4.model.Switch
import org.change.v2.p4.model.parser.{ExtractStatement, ReturnSelectStatement, ReturnStatement, SetStatement}

import scala.collection.JavaConversions._

case class NewParserStrategy(switch: Switch) extends Instruction {
  private lazy val startState = switch.getParserState("start")
  override def isTool: Boolean = true

  override def apply(s: State, verbose: Boolean): (List[State], List[State]) = {
//    startState.getStatements.map(st => st match {
//      case ReturnStatement =>
//      case ReturnSelectStatement =>
//      case ExtractStatement =>
//      case SetStatement =>
//      case _ => ???
//    })
    (Nil, Nil)
  }

}
