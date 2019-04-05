package org.change.p4.control.querylib

import org.change.p4.control.MkQuery
import org.change.p4.control.SMInstantiator._
import org.change.p4.control.queryimpl.{P4RootMemory, QueryBuilder}
import org.change.p4.model.Switch
import org.change.p4.model.actions.P4ActionCall
import org.change.p4.model.actions.P4ActionCall.ParamExpression
import org.change.p4.model.control.IfElseStatement
import org.change.p4.model.parser.CaseEntry
import z3.scala.Z3Context

import scala.collection.JavaConverters._

class IsValidQuery(switch: Switch, context: Z3Context)
    extends QueryBuilder(switch, context) {
  override def query(node: Object, memory: P4RootMemory): Option[P4RootMemory] =
    node match {
      case (ifElseStatement: IfElseStatement, _, _) =>
        val fail = memory.validityFailure(ifElseStatement.getCondition)
        Some(
          memory
            .where(fail)
            .fails(
              "invalid header access in condition of if condition " +
                s"${ifElseStatement.getCondition.toString}"
            )
            .as[P4RootMemory]
        )
      case (caseEntry: CaseEntry, _, _) =>
        val bexpr = caseEntry.getBExpr
        val fail = memory.validityFailure(bexpr)
        Some(
          memory
            .where(fail)
            .fails(
              "invalid header access in condition of case entry" +
                s"${bexpr.toString}"
            )
            .as[P4RootMemory]
        )
      case acall: P4ActionCall =>
        val fail = memory.or(
          acall
            .params()
            .asScala
            .map(_.asInstanceOf[ParamExpression])
            .map(p => {
              MkQuery.validityFailure(memory, p.getExpression)
            })
        )
        Some(
          memory
            .where(fail)
            .fails(
              s"invalid header access in action call " +
                s"${acall.toString}"
            )
            .as[P4RootMemory]
        )
      case _ => None
    }
}
