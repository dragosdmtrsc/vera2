package org.change.v2.p4.model.updated.control.control_statements

import org.change.v2.p4.model.updated.control.ControlStatement
import org.change.v2.p4.model.updated.control.control_statements.case_list.CaseList

case class ApplyAndSelectBlock(
                              tableName: String,
                              caseList: Option[CaseList]
                              ) extends ControlStatement {

}
