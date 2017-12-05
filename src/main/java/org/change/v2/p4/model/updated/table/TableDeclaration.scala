package org.change.v2.p4.model.updated.table

import org.change.v2.p4.model.updated.ActionRef

case class TableDeclaration(
                           tableName: String,
                           allowedActions: Iterable[ActionRef],
                           tableMatches: Iterable[TableMatch]
                           ) {

}
