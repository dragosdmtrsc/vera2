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
      Some(memory.where(fail).fails("invalid condition in if condition" +
        s"${ifElseStatement.getCondition.toString}").as[P4RootMemory])
    case (caseEntry: CaseEntry, _, _) =>
      val bexpr = caseEntry.getBExpr
      val fail = memory.validityFailure(bexpr)
      Some(memory.where(fail).fails("invalid condition in case entry" +
        s"${bexpr.toString}").as[P4RootMemory])
    case acall : P4ActionCall =>
      val fail = memory.or(acall.params().asScala
        .map(_.asInstanceOf[ParamExpression])
        .map(p => {
          MkQuery.validityFailure(memory, p.getExpression)
        }))
      Some(memory.where(fail).fails(s"invalid action call in " +
        s"${acall.toString}").as[P4RootMemory])
    case _ => None
  }
}

class IndexOutOfBounds(switch: Switch,
                       context : Z3Context) extends QueryBuilder(switch, context) {
  override def query(node : Object,
                     memory : P4RootMemory) : Option[P4RootMemory] = node match {
    case (ifElseStatement: IfElseStatement, _, _) =>
      val fail = memory.validityFailure(ifElseStatement.getCondition)
      Some(memory.where(fail).fails("array index out of bounds in if statement condition " +
        s"${ifElseStatement.getCondition.toString}").as[P4RootMemory])
    case (caseEntry: CaseEntry, _, _) =>
      val bexpr = caseEntry.getBExpr
      val fail = memory.validityFailure(bexpr)
      Some(memory.where(fail).fails("array index out of bounds in case statement " +
        s"${bexpr.toString}").as[P4RootMemory])
    case acall : P4ActionCall =>
      val fail = memory.or(acall.params().asScala
        .map(_.asInstanceOf[ParamExpression])
        .map(p => {
          MkQuery.validityFailure(memory, p.getExpression)
        }))
      Some(memory.where(fail).fails("array index out of bounds in action call " +
        s"${acall.toString}").as[P4RootMemory])
    case _ => None
  }
}

class DisjQuery(switch: Switch,
                context: Z3Context,
                q1 : QueryBuilder,
                q2 : QueryBuilder) extends QueryBuilder(switch, context) {
  override def query(evt: Object, in: P4RootMemory): Option[P4RootMemory] = {
    val qa = q1.query(evt, in)
    val qb = q2.query(evt, in)
    (qa, qb) match {
      case (Some(lr), Some(rr)) =>
        Some((lr || rr).as[P4RootMemory])
      case (Some(lr), None) =>
        Some(lr)
      case (None, Some(rr)) =>
        Some(rr)
      case (None, None) =>
        None
    }
  }
}

object DisjQuery {
  def or(switch: Switch, context: Z3Context)
        (it : Iterable[QueryBuilder]) : QueryBuilder = {
    if (it.isEmpty)
      throw new IllegalArgumentException("disj query must have at least one query")
    else if (it.size == 1)
      it.head
    else {
      it.tail.foldLeft(it.head)((acc, x) => {
        new DisjQuery(switch, context, acc, x)
      })
    }
  }
  def apply(switch: Switch, context: Z3Context)
           (qbs: QueryBuilder*) : QueryBuilder =
    or(switch, context)(qbs.toIterable)
}