package org.change.v2.analysis

import org.change.v2.analysis.expression.abst.FloatingExpression
import org.change.v2.analysis.expression.concrete.nonprimitive._
import org.change.v2.analysis.processingmodels.instructions._
import org.change.v2.analysis.processingmodels.{
  Instruction,
  Let,
  SuperFork,
  Unfail
}

case class RWVars(read: Set[String] = Set.empty,
                  write: Set[String] = Set.empty) {
  def +(vs: RWVars): RWVars = RWVars(read ++ vs.read, write ++ vs.write)
  def addRead(v: String): RWVars = copy(read + v)
  def addReads(v: Set[String]): RWVars = copy(read ++ v)
  def addWrites(v: Set[String]): RWVars = copy(write ++ v)
}

case class Topology(instructions: Map[String, Instruction]) {

  def vars(port: String): RWVars = vars(instructions.getOrElse(port, NoOp))
  def vars(floatingConstraint: FloatingConstraint): Set[String] =
    floatingConstraint match {
      case :|:(a, b) => vars(a) ++ vars(b)
      case :&:(a, b) => vars(a) ++ vars(b)
      case :~:(c)    => vars(c)
      case :==:(exp) => vars(exp)
      case :<:(exp)  => vars(exp)
      case :<=:(exp) => vars(exp)
      case :>:(exp)  => vars(exp)
      case :>=:(exp) => vars(exp)
      case _         => Set.empty
    }
  def vars(floatingExpression: FloatingExpression): Set[String] =
    floatingExpression match {
      case :<<:(left, right) => vars(left) ++ vars(right)
      case :!:(left)         => vars(left)
      case Symbol(id)        => Set(id)
      case :||:(left, right) => vars(left) ++ vars(right)
      case :^:(left, right)  => vars(left) ++ vars(right)
      case :&&:(left, right) => vars(left) ++ vars(right)
      case :+:(left, right)  => vars(left) ++ vars(right)
      case :-:(left, right)  => vars(left) ++ vars(right)
      case _                 => Set.empty
    }

  def vars(instruction: Instruction): RWVars = instruction match {
    case Unfail(instruction) => vars(instruction)
    case DestroyPacket()     => RWVars()
    case Fork(forkBlocks) =>
      forkBlocks.foldLeft(RWVars())((acc, x) => {
        acc + vars(x)
      })
    case InstructionBlock(instructions) =>
      instructions.foldLeft(RWVars())((acc, x) => {
        acc + vars(x)
      })
    case AllocateSymbol(id, size)  => RWVars(write = Set(id))
    case DeallocateNamedSymbol(id) => RWVars(write = Set(id), read = Set(id))
    case If(testInstr, thenWhat, elseWhat) =>
      vars(testInstr) + vars(thenWhat) + vars(elseWhat)
    case SuperFork(instructions) =>
      instructions.foldLeft(RWVars())((acc, x) => {
        acc + vars(x)
      })
    case ConstrainNamedSymbol(id, dc, c) =>
      RWVars(read = Set(id)).addReads(vars(dc))
    case Let(string, instruction) => vars(instruction)
    case AssignNamedSymbol(id, exp, t) =>
      RWVars(write = Set(id)).addReads(vars(exp))
    case ExistsNamedSymbol(symbol) => RWVars(read = Set(symbol))
    case ConstrainFloatingExpression(floatingExpression, dc) =>
      RWVars(read = vars(floatingExpression)).addReads(vars(dc))
    case ConstrainRaw(a, dc, c) => RWVars(read = vars(dc))
    case AssignNamedSymbolWithLength(id, exp, width) =>
      RWVars(write = Set(id)).addReads(vars(exp))
    case NotExistsNamedSymbol(symbol) => RWVars(read = Set(symbol))
    case _                            => RWVars()
  }
  lazy val m = instructions.mapValues(r => {
    vars(r)
  })

  def allVars(): RWVars = m.foldLeft(RWVars())((acc, x) => {
    x._2 + acc
  })
}
