package org.change.v2.util

import org.change.v2.analysis.processingmodels.Instruction
import org.change.v2.analysis.processingmodels.instructions._

object Neg {
  def apply(instr : Instruction) : Instruction = instr match {
    case InstructionBlock(is) => Fork(is.map(apply))
    case Fork(is) => InstructionBlock(is.map(apply))
    case ConstrainNamedSymbol(a, x, _) => ConstrainNamedSymbol(a, :~:(x))
    case ConstrainRaw(a, x, _) => ConstrainRaw(a, :~:(x))
    case ConstrainFloatingExpression(fe, x) => ConstrainFloatingExpression(fe, :~:(x))
  }
}
