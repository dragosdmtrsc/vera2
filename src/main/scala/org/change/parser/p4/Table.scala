package org.change.parser.p4

import org.change.v2.analysis.processingmodels.Instruction
import org.change.v2.analysis.processingmodels.instructions.{InstructionBlock, NoOp}
import org.change.v2.p4.model.{Switch, SwitchInstance}
import scala.collection.JavaConversions._
/**
  * Created by dragos on 04.09.2017.
  */
case class EnterTable(tableName : String,
                 switchInstance: SwitchInstance) {

  override def toString: String = s"${switchInstance.getName}.table.$tableName.in"
  val switch : Switch = switchInstance.getSwitchSpec
  val defaultAction = switchInstance.getDefaultAction(tableName)
  val actions = switchInstance.flowInstanceIterator(tableName)

  def symnetCode() : Instruction = {
    InstructionBlock(
      actions.zipWithIndex.map(x => {
          new EnterFlowEntry(tableName,
            x._2,
            switchInstance,
            new FireAction(x._1.getTable, x._2, switchInstance).symnetCode()
          ).symnetCode()
      })
    )
  }

}

case class EnterFlowEntry(tableName : String,
                          flowNumber : Int,
                          switchInstance: SwitchInstance,
                          action : Instruction) {
  override def toString: String = s"${switchInstance.getName}.flow.$tableName.$flowNumber.in"

  val switch : Switch = switchInstance.getSwitchSpec
  val defaultAction = switchInstance.getDefaultAction(tableName)
  val instance = switchInstance.flowInstanceIterator(tableName)(flowNumber)

  def symnetCode() : Instruction = {
    InstructionBlock(
      NoOp,
      action
    )
  }

}

