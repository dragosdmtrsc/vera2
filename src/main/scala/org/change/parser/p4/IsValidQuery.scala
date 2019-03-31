package org.change.parser.p4

import org.change.parser.p4.control.SMInstantiator._
import org.change.parser.p4.control.queryimpl.{P4RootMemory, QueryBuilder}
import org.change.parser.p4.control.{MkQuery, P4Memory, P4Query}
import org.change.v2.p4.model.Switch
import org.change.v2.p4.model.actions.P4ActionCall.ParamExpression
import org.change.v2.p4.model.actions.{P4Action, P4ActionCall, P4ActionParameterType}
import org.change.v2.p4.model.control.IfElseStatement
import org.change.v2.p4.model.parser.CaseEntry
import z3.scala.Z3Context

import scala.collection.JavaConverters._

class IsValidQuery(switch: Switch,
                   context : Z3Context) extends QueryBuilder(switch, context) {
  override def query(node : Object,
                     memory : P4RootMemory) : Option[P4RootMemory] = node match {
    case (ifElseStatement: IfElseStatement, _, _) =>
      val fail = memory.validityFailure(ifElseStatement.getCondition)
      Some(memory.where(
        fail
      ).as[P4RootMemory])
    case (caseEntry: CaseEntry, _, _) =>
      val bexpr = caseEntry.getBExpr
      val fail = memory.validityFailure(bexpr)
      Some(memory.where(fail).as[P4RootMemory])
    case acall : P4ActionCall =>
      val fail = memory.or(acall.params().asScala
        .map(_.asInstanceOf[ParamExpression])
        .map(p => {
          MkQuery.validityFailure(memory, p.getExpression)
        }))
      Some(memory.where(fail).as[P4RootMemory])
    case _ => None
  }
}
