package org.change.p4.control

import com.microsoft.z3.Expr
import org.change.p4.control.types.P4Type
import org.change.p4.model.table.TableMatch

trait IFlowInstance {
  def exists(): Expr
  def hits(): Expr
  def actionRun(): Expr
  def getMatchValue(tableMatch: TableMatch): Expr
  def getMatchMask(tableMatch: TableMatch): Expr
  def getActionParam(action: String, parmName: String): Expr
  def getActionParamType(action: String, parmName: String): P4Type
}
