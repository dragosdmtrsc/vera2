package org.change.v2.p4.model.actions.symbolic.primitive

import org.change.v2.p4.model.actions.P4Action
import org.change.v2.p4.model.actions.primitives.ModifyField
import org.change.v2.p4.model.actions.symbolic.SymbolicAction

abstract class SymbolicModifyField(val symbolicParameterIds: Seq[String]) extends SymbolicAction {

  
  
}

object SymbolicModifyField {
  /**
    * TODO: Mask is not supported.
    * @param initialAction
    * @return
    */
  def apply(initialAction: P4Action): SymbolicModifyField = ??? // TODO: new SymbolicModifyField(Seq("dest"))
}