package org.change.v2.analysis.equivalence

import java.util.UUID

import org.change.v2.abstractnet.mat.condition.Intersect
import org.change.v2.analysis.constraint._
import org.change.v2.analysis.executor.{CodeAwareInstructionExecutor, OVSExecutor}
import org.change.v2.analysis.executor.solvers.{Solver, Z3BVSolver}
import org.change.v2.analysis.executor.translators.{Translator, Z3BVTranslator}
import org.change.v2.analysis.expression.abst.Expression
import org.change.v2.analysis.expression.concrete.{ConstantBValue, ConstantStringValue, ConstantValue, SymbolicValue}
import org.change.v2.analysis.expression.concrete.nonprimitive._
import org.change.v2.analysis.memory.{MemorySpace, State, Value}
import org.change.v2.analysis.processingmodels.instructions.{Assign, Fork, InstructionBlock}
import org.change.v2.analysis.processingmodels.{Instruction, SuperFork}
import z3.scala.{Z3AST, Z3Config, Z3Context, Z3Solver}

import scala.collection.mutable

class FullZ3BVTranslator(context: Z3Context) extends Z3BVTranslator(context = context) {
  override def translate(mem: MemorySpace): Z3Solver =
    (mem.symbols.values ++ mem.rawObjects.values).foldLeft(context.mkSolver())((slv, mo) =>
      mo.valueStack.foldLeft(slv)((acc, vs) => {
        vs.vs.foldLeft(acc)((acc2, v) => translate(acc2, v, mo.size)._2)
      })
    )

  override def translate(slv: Z3Solver, e: Expression, size: Int): (Z3AST, Z3Solver) = e match {
    case s : SymbolicValue => (context.mkConst("sym" + s.id, context.mkBVSort(size)), slv)
    case _ => super.translate(slv, e, size)
  }
}

class FullZ3Solver extends Z3BVSolver {
  override def translate(mem: MemorySpace): Z3Solver = {
    val z3 = createContext
    val transl = new FullZ3BVTranslator(z3)
    transl.translate(mem)
  }
}

class CodeAwareInstructionExecutor2(program : Map[String, Instruction],
                                    solver : Solver) extends
  CodeAwareInstructionExecutor(program, solver) {
}