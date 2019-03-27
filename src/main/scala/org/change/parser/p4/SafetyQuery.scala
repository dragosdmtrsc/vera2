package org.change.parser.p4

import org.change.parser.p4.control.{P4Memory, P4Query}
import org.change.parser.p4.control.SMInstantiator._
import org.change.v2.p4.model.actions.{P4ActionCall, P4ActionParameterType}
import org.change.v2.p4.model.actions.P4ActionCall.ParamExpression
import org.change.v2.p4.model.actions.primitives.ModifyField
import org.change.v2.p4.model.control.IfElseStatement
import org.change.v2.p4.model.parser.CaseEntry

import scala.collection.JavaConverters._

class SafetyQuery {
  def on(node : Object, memory: P4Memory) : P4Query = {
    memory.boolVal(false)
  }
}
class IsValidQuery extends SafetyQuery {
  override def on(node: Object, memory: P4Memory): P4Query = node match {
    case ifElseStatement: IfElseStatement =>
      memory.validityFailure(ifElseStatement.getCondition)
    case caseEntry: CaseEntry =>
      memory.validityFailure(caseEntry.getBExpr)
    case act: P4ActionCall =>
      memory.and(act.getP4Action.getParameterList.asScala.zip(act.params().asScala).filter(pv => {
        P4ActionParameterType.isField(pv._1.getType)
      }).map(pv => {
        memory.validityFailure(pv._2.asInstanceOf[ParamExpression].getExpression)
      }))
    case _ => memory.boolVal(false)
  }
}
