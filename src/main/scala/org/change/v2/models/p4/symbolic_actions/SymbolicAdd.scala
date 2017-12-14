package org.change.v2.models.p4.symbolic_actions

import org.change.v2.analysis.expression.abst.{Expression, FloatingExpression}
import org.change.v2.analysis.expression.concrete.nonprimitive.:+:
import org.change.v2.analysis.processingmodels.Instruction
import org.change.v2.analysis.processingmodels.instructions.Assign
import org.change.v2.models.ProducesSEFL
import org.change.v2.p4.model.actions.primitives.Add

class SymbolicAdd(
                   dest: String,
                   valueBindings: Map[String, FloatingExpression]
                 ) extends Add with ProducesSEFL {

  val value1 ="value1"
  val value2 = "value2"

  // The two params are bound.
  assert(valueBindings.contains(value1) && valueBindings.contains(value2))

  override def toSEFL[A](extraInfo: Option[A]): Instruction =
    Assign(dest, :+:(valueBindings(value1), valueBindings(value2)))
}
