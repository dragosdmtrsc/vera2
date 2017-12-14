package org.change.v2.models.p4.symbolic_actions

import org.change.v2.analysis.expression.abst.FloatingExpression
import org.change.v2.analysis.expression.concrete.nonprimitive.{:+:, :@}
import org.change.v2.analysis.processingmodels.instructions.Assign
import org.change.v2.models.ProducesSEFL
import org.change.v2.p4.model.actions.primitives.AddToField

case class SymbolicAddToField(
                             dest: String,
                             value: FloatingExpression
                             ) extends AddToField with ProducesSEFL{
  override def toSEFL[A](extraInfo: Option[A]) =
    Assign(dest, :+:(:@(dest), value))
}
