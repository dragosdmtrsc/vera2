package org.change.v2.p4.model.updated.control.control_statements

import org.change.v2.p4.model.updated.control.{ControlBlock, ControlStatement}

case class IfElse(
                 testExpr: P4BooleanExpression,
                 thenControlBlock: ControlStatement,
                 elseStatement: Option[ControlStatement]
                 ) extends ControlStatement {

}
