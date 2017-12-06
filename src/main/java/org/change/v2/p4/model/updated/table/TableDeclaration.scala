package org.change.v2.p4.model.updated.table

case class TableDeclaration(
                           tableName: String,
                           allowedActions: ActionSpecification,
                           tableMatches: Iterable[TableMatch]
                           ) {

}
