package org.change.v2.models.p4

import org.change.v2.analysis.expression.abst.Expression
import org.change.v2.analysis.expression.concrete.SymbolicValue
import org.change.v2.p4.model.Switch
import org.change.v2.p4.model.actions.P4Action

case class SymbolicTable(
                          tableName: String,
                          actions: Iterable[P4Action],
                          actionParameters: Map[String, Expression],
                          tableMatchParameters: Map[String, Expression]
                        )

object SymbolicTable {
  import collection.JavaConverters._

  def fromSwitch(switch: Switch): Map[String,SymbolicTable] = {
    for {
      table <- switch.getDeclaredTables.asScala
      tableActionIds = switch.getAllowedActions(table)
      tableActions = tableActionIds.asScala.map(switch.getActionRegistrar.getAction(_))
      tableMatches = switch.getTableMatches(table).asScala
    } yield {
      table -> SymbolicTable(
        tableName = table,
        actions = tableActions,
        actionParameters = {
          for {
            action <- tableActions
            parameter <- action.getParameterList.asScala
          } yield s"$table.${action.getActionName}.parameter.${parameter.getParamName}" -> SymbolicValue()
        }.toMap,
        tableMatchParameters = {
          for {
            tableMatch <- tableMatches
            action <- tableActions
          } yield s"$table.${action.getActionName}.matchValue.${tableMatch.getKey}" -> SymbolicValue()
        }.toMap
      )
    }
  }.toMap
}
