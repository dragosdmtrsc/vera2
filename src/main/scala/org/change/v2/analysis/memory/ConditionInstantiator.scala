package org.change.v2.analysis.memory

import org.change.v2.analysis.constraint._
import org.change.v2.analysis.expression.abst.{Expression, FloatingExpression}
import org.change.v2.analysis.expression.concrete.{ConstantBValue, ConstantStringValue, ConstantValue, SymbolicValue}
import org.change.v2.analysis.expression.concrete.nonprimitive._
import org.change.v2.analysis.processingmodels.Instruction
import org.change.v2.analysis.processingmodels.instructions._

object ConditionInstantiator {

  def apply(constraint : Constraint, against : State) : Constraint = constraint match {
    case LT_E(e) => LT_E(apply(e, against))
    case LTE_E(e) => LTE_E(apply(e, against))
    case GTE_E(e) => GTE_E(apply(e, against))
    case GT_E(e) => GT_E(apply(e, against))
    case EQ_E(e) => EQ_E(apply(e, against))
    case No => No
    case LT(v) => LT(v)
    case LTE(v) => LTE(v)
    case GT(v) => GT(v)
    case GTE(v) => GTE(v)
    case E(v) => E(v)
    case Truth() => Truth()
    case Range(v1, v2) => Range(v1, v2)
    case OR(constraints) => OR(constraints.map(apply(_, against)))
    case AND(constraints) => AND(constraints.map(apply(_, against)))
    case NOT(constraint) => NOT(apply(constraint, against))
  }

  def apply(expression : Expression, against : State) : Expression = expression match {
    case Concat(expressions) => Concat(expressions.map(r => {
      r.copy(valueStack = ValueStack.empty.addDefinition(Value(apply(r.value.get.e, against))) :: Nil)
    }))
    case LShift(a, b) =>LShift(Value(apply(a.e, against)), Value(apply(b.e, against)))
    case Reference(what, name) => apply(what.e, against)
    case SymbolicValue(name) => against.memory.eval(name).map(_.e).get
    case Plus(a, b) => Plus(Value(apply(a.e, against)), Value(apply(b.e, against)))
    case LNot(a) => LNot(a)
    case LAnd(a, b) => LAnd(Value(apply(a.e, against)), Value(apply(b.e, against)))
    case Lor(a, b) => Lor(Value(apply(a.e, against)), Value(apply(b.e, against)))
    case LXor(a, b) => LXor(Value(apply(a.e, against)), Value(apply(b.e, against)))
    case Minus(a, b) => MinusE(apply(a.e, against), apply(b.e, against))
    case PlusE(a, b) => PlusE(apply(a, against), apply(b, against))
    case MinusE(a, b) => MinusE(apply(a, against), apply(b, against))
    case LogicalOr(a, b) => LogicalOr(Value(apply(a.e, against)), Value(apply(b.e, against)))
    case c : ConstantBValue => c
    case c : ConstantStringValue => c
    case c : ConstantValue => c
  }

  def apply(condition : Condition, against : State) : Condition = condition match {
    case OP(expression, constraint, size) => OP(apply(expression, against), apply(constraint, against), size)
    case FAND(conditions) => FAND(conditions.map(apply(_, against)))
    case FOR(conditions) => FOR(conditions.map(apply(_, against)))
    case FNOT(condition) => FNOT(apply(condition, against))
    case TRUE => TRUE
    case FALSE => FALSE
  }

}

object InstructionCrawler {
  def crawlConstraint(floatingConstraint: FloatingConstraint) : (Set[String], Set[String]) =
    floatingConstraint match {
      case :|:(a, b) => val as = crawlConstraint(a)
        val bs = crawlConstraint(b)
        (as._1 ++ bs._1, as._2 ++ bs._2)
      case :&:(a, b) => val as = crawlConstraint(a)
        val bs = crawlConstraint(b)
        (as._1 ++ bs._1, as._2 ++ bs._2)
      case :~:(a) => crawlConstraint(a)
      case :==:(exp) => crawlExpression(exp)
      case :<:(exp) => crawlExpression(exp)
      case :<=:(exp) => crawlExpression(exp)
      case :>:(exp) => crawlExpression(exp)
      case :>=:(exp) => crawlExpression(exp)
      case _ => (Set.empty, Set.empty)
    }

  def crawlExpression(floatingExpression : FloatingExpression): (Set[String], Set[String])  =
    floatingExpression match {
      case :<<:(left, right) => val as = crawlExpression(left)
        val bs = crawlExpression(right)
        (as._1 ++ bs._1, as._2 ++ bs._2)
      case :!:(left) => crawlExpression(left)
      case Symbol(id) => (Set(id), Set.empty)
      case :||:(left, right) => val as = crawlExpression(left)
        val bs = crawlExpression(right)
        (as._1 ++ bs._1, as._2 ++ bs._2)
      case :^:(left, right) => val as = crawlExpression(left)
        val bs = crawlExpression(right)
        (as._1 ++ bs._1, as._2 ++ bs._2)
      case :&&:(left, right) => val as = crawlExpression(left)
        val bs = crawlExpression(right)
        (as._1 ++ bs._1, as._2 ++ bs._2)
      case :+:(left, right) => val as = crawlExpression(left)
        val bs = crawlExpression(right)
        (as._1 ++ bs._1, as._2 ++ bs._2)
      case :-:(left, right) => val as = crawlExpression(left)
        val bs = crawlExpression(right)
        (as._1 ++ bs._1, as._2 ++ bs._2)
      case _ => (Set.empty, Set.empty)
    }

  def crawlInstruction(instruction : Instruction): (Set[String], Set[String]) = instruction match {
    case Fork(forkBlocks) => val a = forkBlocks.map(r => crawlInstruction(r)).unzip
      (a._1.flatten.toSet, a._2.flatten.toSet)
    case InstructionBlock(instructions) => val a = instructions.map(r => crawlInstruction(r)).unzip
      (a._1.flatten.toSet, a._2.flatten.toSet)
    case AllocateSymbol(id, size) => (Set.empty, Set(id))
    case DeallocateNamedSymbol(id) => (Set.empty, Set(id))
    case If(testInstr, thenWhat, elseWhat) => val a = List(testInstr, thenWhat, elseWhat).map(r => crawlInstruction(r)).unzip
      (a._1.flatten.toSet, a._2.flatten.toSet)
    case ConstrainNamedSymbol(id, dc, c) => val d = crawlConstraint(dc)
      (d._1 + id, d._2)
    case AssignNamedSymbol(id, exp, t) => val d = crawlExpression(exp)
      (d._1, d._2 + id)
    case ExistsNamedSymbol(symbol) => (Set(symbol), Set.empty)
    case ConstrainFloatingExpression(floatingExpression, dc) =>
      val e = crawlExpression(floatingExpression)
      val d = crawlConstraint(dc)
      (e._1 ++ d._1, e._2 ++ d._2)
    case AssignNamedSymbolWithLength(id, exp, width) => val d = crawlExpression(exp)
      (d._1, d._2 + id)
    case NotExistsNamedSymbol(symbol) => (Set(symbol), Set.empty)
    case _ => (Set.empty, Set.empty)
  }
}