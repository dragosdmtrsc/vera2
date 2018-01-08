package org.change.v2.p4.model.updated.table

import org.change.v2.p4.model.updated.ActionRef

trait ActionSpecification {

}

case class BasicActionSpecification(
                                   actionRefs: Iterable[ActionRef]
                                   ) extends ActionSpecification