package org.change.v2.p4.model.updated.control.control_statements.case_list

import org.change.v2.p4.model.updated.ActionRef

case class DefaultActionRef(
                           tableName: Option[String]
                           ) extends ActionRef {
  override def name = tableName match {
    case Some(tableRef) => s"default-action-$tableRef"
    case None => "default-action-unknown"
  }
}
