package org.change.v2.p4.model.actions.symbolic

import org.change.v2.abstractnet.SEFLable
import org.change.v2.analysis.expression.abst.Expression
import org.change.v2.analysis.processingmodels.Instruction
import org.change.v2.analysis.processingmodels.instructions.NoOp
import org.change.v2.p4.model.actions.P4Action

trait SymbolicAction extends SEFLable {

  def arguments: Map[String, Expression]

}

object SymbolicAction {
  def fromConcreteAction(
                          action: P4Action,
                          concreteArguments: Map[String, Long]) = action match {
    case _ => ???
  }
}