package org.change.v2.p4.model.updated.control.control_statements.case_list

import org.change.v2.p4.model.updated.ActionRef

case class NamedActionRef(
                         name: String,
                         tableName: Option[String]
                         ) extends ActionRef {

                         }
