package org.change.v2.analysis.executor

import org.change.v2.analysis.executor.solvers.Solver
import org.change.v2.analysis.memory.State
import org.change.v2.analysis.processingmodels.Instruction
import org.change.v2.analysis.processingmodels.instructions.{:==:, ConstrainNamedSymbol, ConstrainRaw, InstructionBlock}

class AMFExecutor(
                                   program : Map[String, Instruction],
                                   solver : Solver) extends CodeAwareInstructionExecutor(program, solver) {

  import org.change.v2.analysis.expression.concrete.nonprimitive.{Symbol, Address}

  override def executeConstrainRaw(instruction: ConstrainRaw, s: State, v: Boolean) = instruction match {
    case ConstrainRaw(target, :==:(Symbol(id)), _) => super.executeInstructionBlock(
      InstructionBlock(
        instruction,
        ConstrainNamedSymbol(id, :==:(Address(target)))
      ), s, v
    )
    case ConstrainRaw(target, :==:(Address(offset)), _) => super.executeInstructionBlock(
      InstructionBlock(
        instruction,
        ConstrainRaw(offset, :==:(Address(target)))
      ), s, v
    )
    case _ => super.executeConstrainRaw(instruction, s, v)
  }


  override def executeConstrainNamedSymbol(
                                            instruction: ConstrainNamedSymbol,
                                            s: State,
                                            v: Boolean) = instruction match {
    case ConstrainNamedSymbol(target, :==:(Symbol(id)), _) => super.executeInstructionBlock(
      InstructionBlock(
        instruction,
        ConstrainNamedSymbol(id, :==:(Symbol(target)))
      ), s, v
    )
    case ConstrainNamedSymbol(target, :==:(Address(offset)), _) => super.executeInstructionBlock(
      InstructionBlock(
        instruction,
        ConstrainRaw(offset, :==:(Symbol(target)))
      ), s, v
    )
    case _ => super.executeConstrainNamedSymbol(instruction, s, v)
  }
}
