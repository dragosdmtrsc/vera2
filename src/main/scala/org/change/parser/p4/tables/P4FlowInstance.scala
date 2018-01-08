package org.change.parser.p4.tables

import org.change.v2.analysis.expression.abst.FloatingExpression

trait ParmInstance
case class LPMMatch(va : FloatingExpression, mask : FloatingExpression) extends ParmInstance
case class RangeMatch(min : FloatingExpression, max : FloatingExpression) extends ParmInstance
case class TernaryMatch(va : FloatingExpression, mask : FloatingExpression) extends ParmInstance
case class ValidMatch(v : FloatingExpression) extends ParmInstance
case class Equal(va : FloatingExpression) extends ParmInstance

case class P4TableDefinition(flowInstances : List[P4FlowInstance] = Nil, defaultAction: ActionDefinition = DropAction)
case class P4FlowInstance(matchParams : Map[String, ParmInstance],
                          action : ActionDefinition)
case class ActionDefinition(action : String, actionParams : Map[String, FloatingExpression])

object DropAction extends ActionDefinition("drop", Map.empty)