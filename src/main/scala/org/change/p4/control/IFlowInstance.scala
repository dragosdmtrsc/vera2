package org.change.p4.control

import org.change.p4.control.types.P4Type
import org.change.p4.model.table.TableMatch
import z3.scala.Z3AST

trait IFlowInstance {
  def exists(): Z3AST
  def hits(): Z3AST
  def actionRun(): Z3AST
  def getMatchValue(tableMatch: TableMatch): Z3AST
  def getMatchMask(tableMatch: TableMatch): Z3AST
  def getActionParam(action: String, parmName: String): Z3AST
  def getActionParamType(action: String, parmName: String): P4Type
}
