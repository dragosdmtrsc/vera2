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


class FullZ3Solver extends Z3BVSolver