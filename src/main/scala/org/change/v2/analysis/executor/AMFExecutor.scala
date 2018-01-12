package org.change.v2.analysis.executor

import org.change.v2.analysis.executor.solvers.Solver
import org.change.v2.analysis.memory.{Intable, State}
import org.change.v2.analysis.processingmodels.Instruction
import org.change.v2.analysis.processingmodels.instructions._

class AMFExecutor(
                                   program : Map[String, Instruction],
                                   solver : Solver) extends CodeAwareInstructionExecutor(program, solver) {

  class FixedNamedConstrain(val old: ConstrainNamedSymbol) extends ConstrainNamedSymbol(old.id, old.dc, old.c)
  class FixedRawConstrain(val old: ConstrainRaw) extends ConstrainRaw(old.a, old.dc, old.c)

  import org.change.v2.analysis.expression.concrete.nonprimitive.{Symbol, Address}

  override def executeConstrainRaw(instruction: ConstrainRaw, s: State, v: Boolean): (List[State], List[State]) = instruction match {
    case c: FixedRawConstrain => super.executeConstrainRaw(c.old, s, v)
    case ConstrainRaw(target, :==:(Symbol(id)), _) => super.executeInstructionBlock(
      InstructionBlock(
        new FixedRawConstrain(instruction),
        new FixedNamedConstrain(ConstrainNamedSymbol(id, :==:(Address(target))))
      ), s, v
    )
    case ConstrainRaw(target, :==:(Address(offset)), _) => super.executeInstructionBlock(
      InstructionBlock(
        new FixedRawConstrain(instruction),
        new FixedRawConstrain(ConstrainRaw(offset, :==:(Address(target))))
      ), s, v
    )
    case _ => super.executeConstrainRaw(instruction, s, v)
  }


  override def executeConstrainNamedSymbol(
                                            instruction: ConstrainNamedSymbol,
                                            s: State,
                                            v: Boolean) = instruction match {
    case c: FixedNamedConstrain => super.executeConstrainNamedSymbol(c.old, s, v)
    case ConstrainNamedSymbol(target, :==:(Symbol(id)), _) => super.executeInstructionBlock(
      InstructionBlock(
        new FixedNamedConstrain(instruction),
        new FixedNamedConstrain(ConstrainNamedSymbol(id, :==:(Symbol(target))))
      ), s, v
    )
    case ConstrainNamedSymbol(target, :==:(Address(offset)), _) => super.executeInstructionBlock(
      InstructionBlock(
        new FixedNamedConstrain(instruction),
        new FixedRawConstrain(ConstrainRaw(offset, :==:(Symbol(target))))
      ), s, v
    )
    case _ => super.executeConstrainNamedSymbol(instruction, s, v)
  }
}
