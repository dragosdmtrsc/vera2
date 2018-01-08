package org.change.v2.p4.model.updated.control.control_statements.case_list

import org.change.v2.p4.model.updated.ActionRef
import org.change.v2.p4.model.updated.control.ControlBlock

case class ActionCase(
                  actionRef: ActionRef,
                  controlBlock: ControlBlock
                ) extends Case {

}
