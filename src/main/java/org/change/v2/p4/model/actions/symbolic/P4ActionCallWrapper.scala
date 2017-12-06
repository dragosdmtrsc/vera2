package org.change.v2.p4.model.actions.symbolic

import org.change.v2.analysis.expression.abst.Expression
import org.change.v2.p4.model.actions.{P4ActionCall, P4ActionType}

class P4ActionCallWrapper(javaP4Action: P4ActionCall) {

  def isPrimitive: Boolean = javaP4Action.getP4Action.getActionType != P4ActionType.Complex

  def arguments: Map[String, Expression] = ???

}
