package org.change.v2.p4.model.updated.table

import org.change.v2.p4.model.table.MatchKind
import org.change.v2.p4.model.updated.ValueRef

case class TableMatch(
                     what: ValueRef,
                     matchKind: MatchKind
                     ) {

}
