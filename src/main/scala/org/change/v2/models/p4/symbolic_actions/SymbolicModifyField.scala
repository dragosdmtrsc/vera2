package org.change.v2.models.p4.symbolic_actions

import org.change.v2.analysis.expression.abst.FloatingExpression
import org.change.v2.analysis.processingmodels.instructions.Assign
import org.change.v2.models.ProducesSEFL
import org.change.v2.p4.model.actions.primitives.ModifyField

class SymbolicModifyField(
                           dest: String,
                           value: FloatingExpression
                         ) extends ModifyField with ProducesSEFL {

  override def toSEFL[A](extraInfo: Option[A]) =
    Assign(dest, value)

}