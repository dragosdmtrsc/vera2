package org.change.v2.p4.model.updated.sefl_compiler.sefl_code

import org.change.v2.analysis.expression.abst.Expression
import org.change.v2.analysis.memory.TagExp
import org.change.v2.analysis.processingmodels.Instruction

trait SEFLCode {

  def instructions: Map[String, Instruction]
  def symbolTable: Map[String, (Expression, Option[TagExp])]

}

case class SEFLCodeContainer (
                              instructions: Map[String, Instruction],
                              symbolTable: Map[String, (Expression, Option[TagExp])]
                             ) extends SEFLCode

object EmptySEFLProgram extends SEFLCode {
  override def instructions = Map.empty

  override def symbolTable = Map.empty
}