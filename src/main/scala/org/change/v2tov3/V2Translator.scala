package org.change.v2tov3

import org.change.parser.p4.parser.StateExpander.{DeparserInstruction, ParsePacket}
import org.change.v2.analysis.constraint.{FAND, TRUE}
import org.change.v2.analysis.executor.translators.Z3BVTranslator
import org.change.v2.analysis.expression.abst.FloatingExpression
import org.change.v2.analysis.expression.concrete._
import org.change.v2.analysis.expression.concrete.nonprimitive._
import org.change.v2.analysis.memory
import org.change.v2.analysis.memory.TagExp.IntImprovements
import org.change.v2.analysis.memory._
import org.change.v2.analysis.processingmodels.instructions._
import org.change.v2.analysis.processingmodels.networkproc._
import org.change.v2.analysis.processingmodels.{Instruction, SuperFork}
import org.change.v3.syntax.{BExpr, BVAdd, BVAnd, BVExpr, BVNot, BVOr, BVShl, BVSub, BoolLiteral, Clone, EQ, GT, GTE, LAnd, LOr, LT, LTE, Literal, Reference, Allocate => alloc3, Assign => asg3, Assume => assume3, CreateTag => ct3, Deallocate => dealloc3, DestroyTag => dt3, Fail => fail3, Fork => fork3, Forward => fw3, Havoc => havoc3, If => if3, Instruction => i3, InstructionBlock => ib3, IntImprovements => ii3, Intable => intable3, NoOp => nop3, Symbol => sym3, Tag => tag3, TagExp => texp3}
import org.change.v3.syntax.{LNot => not3}
import org.change.v3.semantics.{context, SimpleMemory => SimpleMemory3, SimpleMemoryObject => smo3}
import scala.collection.Map

object V2Translator {

  def mem3mem2(simpleMemory: SimpleMemory3) : SimpleMemory = new SimpleMemory(
    history = simpleMemory.history,
    errorCause = simpleMemory.errorCause,
    memTags = simpleMemory.memTags,
    rawObjects = simpleMemory.rawObjects.mapValues(mo => SimpleMemoryObject(
      ConstantStringValue(mo.ast.toString), mo.size)),
    symbols = simpleMemory.symbols.mapValues(mo => SimpleMemoryObject(
      ConstantStringValue(mo.ast.toString), mo.size)),
    branchHistory = Nil
  )

  def mem2mem3(simpleMemory : SimpleMemory) : SimpleMemory3 = {
    val dummy = context.mkSolver()
    val z3translator = new memory.SimpleMemory.Translator(context, dummy)

    z3translator.createAST(simpleMemory.pathCondition.cd)
    SimpleMemory3(
      history = simpleMemory.history,
      errorCause = simpleMemory.errorCause,
      memTags = simpleMemory.memTags,
      pathCondition = simpleMemory.pathCondition.cd match {
        case FAND(cds) => cds.map(z3translator.createAST)
        case _ => z3translator.createAST(simpleMemory.pathCondition.cd) :: Nil
      }, rawObjects = simpleMemory.rawObjects.mapValues(mo =>
        smo3.apply(z3translator.translateE(mo.size, mo.expression), mo.size)
      ), symbols = simpleMemory.symbols.mapValues(mo => {
        smo3.apply(z3translator.translateE(mo.size, mo.expression), mo.size)
      })
    )
  }

  def mapIntable(intable : Intable) : intable3 = intable match {
    case Tag(name) => tag3(name)
    case TagExp(plusTags, minusTags, rest) => texp3(plusTags.map(mapIntable).map(_.asInstanceOf[tag3]),
      minusTags.map(mapIntable).map(_.asInstanceOf[tag3]), rest)
    case v : IntImprovements => ii3(v.value)
  }

  def mapfex(floatingExpression: FloatingExpression) : BVExpr = floatingExpression match {
    case SymbolicValue(name) => if (name.isEmpty) havoc3("tmp") else havoc3(name)
    case ConstantValue(value, isIp, isMac) => Literal(BigInt(value))
    case :<<:(left, right) => BVShl(mapfex(left), mapfex(right))
    case :!:(left) => BVNot(mapfex(left))
    case Symbol(id) => sym3(id)
    case :||:(left, right) => BVOr(mapfex(left), mapfex(right))
    case :&&:(left, right) => BVAnd(mapfex(left), mapfex(right))
    case :+:(left, right) => BVAdd(mapfex(left), mapfex(right))
    case Havoc(prefix) => havoc3(prefix)
    case Address(a) => Reference(mapIntable(a))
    case :-:(left, right) => BVSub(mapfex(left), mapfex(right))
    case ConstantBValue(h, _) => Literal(if (h.startsWith("#x"))
      BigInt.apply(h.replace("#x", ""), 16)
    else
      BigInt.apply(h, 16))
    case ConstantStringValue(value) => Literal(value.hashCode)
  }

  def cond2cond3(instruction: Instruction) : BExpr = instruction match {
    case InstructionBlock(is) => val c3 = is.map(cond2cond3)
      c3.tail.foldLeft(c3.head)((acc, x) => LAnd(acc, x))
    case Fork(f) => val c3 = f.map(cond2cond3)
      c3.tail.foldLeft(c3.head)((acc, x) => LOr(acc, x))
    case ConstrainRaw(a, b, _) => cond2cond3(ConstrainFloatingExpression(:@(a), b))
    case ConstrainNamedSymbol(a, b, _) => cond2cond3(ConstrainFloatingExpression(:@(a), b))
    case ConstrainFloatingExpression(fe, dc) =>
      def mapCt(dc : FloatingConstraint) : BExpr = dc match {
        case :|:(a, b) => cond2cond3(Fork(ConstrainFloatingExpression(fe, a), ConstrainFloatingExpression(fe, b)))
        case :&:(a, b) => cond2cond3(InstructionBlock(ConstrainFloatingExpression(fe, a), ConstrainFloatingExpression(fe, b)))
        case Yes() => BoolLiteral(true)
        case :~:(c) => not3(mapCt(c))
        case :==:(exp) => EQ(mapfex(fe), mapfex(exp))
        case :>:(exp) => GT(mapfex(fe), mapfex(exp))
        case :>=:(exp) => GTE(mapfex(fe), mapfex(exp))
        case :<:(exp) => LT(mapfex(fe), mapfex(exp))
        case :<=:(exp) => LTE(mapfex(fe), mapfex(exp))
      }
      mapCt(dc)
  }

  def instr2instr3(instruction: Instruction) : i3 = instruction match {
    case Fork(forkBlocks) => fork3(forkBlocks.map(instr2instr3))
    case InstructionBlock(instructions) => ib3(instructions.map(instr2instr3))
    case DestroyTag(name) => dt3(name)
    case Fail(errMsg) => fail3(errMsg)
    case NoOp => nop3
    case Forward(place) => fw3(place)
    case AllocateSymbol(id, size) => alloc3(sym3(id), size)
    case CreateTag(name, value) => ct3(name, mapIntable(value))
    case DeallocateNamedSymbol(id) => dealloc3(sym3(id))
    case If(testInstr, thenWhat, elseWhat) => if3(cond2cond3(testInstr), instr2instr3(thenWhat), instr2instr3(elseWhat))
    case SuperFork(instructions) => Clone(instructions.map(instr2instr3))
    case ConstrainNamedSymbol(id, dc, _) => instr2instr3(Assume(ConstrainFloatingExpression(:@(id), dc)))
    case AssignNamedSymbol(id, exp, t) => asg3(sym3(id), mapfex(exp))
    case DeallocateRaw(a, size) => dealloc3(Reference(mapIntable(a)))
    case Call(place) => fw3(place)
    case AllocateRaw(a, size) => alloc3(Reference(mapIntable(a)), size)
    case AssignRaw(a, exp, t) => asg3(Reference(mapIntable(a)), mapfex(exp))
    case cfe : ConstrainFloatingExpression => instr2instr3(Assume(cfe))
    case Assume(i) => assume3(cond2cond3(i))
    case ConstrainRaw(a, dc, _) => instr2instr3(Assume(ConstrainFloatingExpression(:@(a), dc)))
  }

  def v2v3(v2Program : Map[String, Instruction]) : Map[String, i3] = v2Program.mapValues(instr2instr3)
}
