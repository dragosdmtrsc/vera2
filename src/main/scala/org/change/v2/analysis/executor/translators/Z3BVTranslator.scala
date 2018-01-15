package org.change.v2.analysis.executor.translators

import org.change.v2.analysis.constraint._
import org.change.v2.analysis.expression.abst.Expression
import org.change.v2.analysis.expression.concrete.{ConstantBValue, ConstantStringValue, ConstantValue, SymbolicValue}
import org.change.v2.analysis.expression.concrete.nonprimitive._
import org.change.v2.analysis.memory.{MemorySpace, Value}
import z3.scala.{Z3AST, Z3Context, Z3Solver}

class Z3BVTranslator(context: Z3Context) extends Translator[Z3Solver] {
  override def translate(mem: MemorySpace): Z3Solver = {
    val slv = context.mkSolver()
    (mem.symbols.values ++ mem.rawObjects.values).foldLeft(slv) { (slv, mo) =>
      mo.value match {
        case Some(v) => this.translate(slv, v, mo.size)._2
        case _ => slv
      }
    }
  }

  def translate(slv: Z3Solver, e: Expression, size : Int): (Z3AST, Z3Solver) = {
    val ast = e match {
      case Plus(a, b) => context.mkBVAdd(translate(slv, a, size)._1, translate(slv, b, size)._1)
      case Minus(a, b) => context.mkBVSub(translate(slv, a, size)._1, translate(slv, b, size)._1)
      case Reference(what, _) => translate(slv, what, size)._1
      case ConstantValue(v, _, _) => context.mkNumeral(v.toString, context.mkBVSort(size))
      case ConstantBValue(v, sz) => context.mkNumeral(BigInt(v.substring(2), 16).toString, context.mkBVSort(size))
      case SymbolicValue(_) => context.mkConst(e.id.toString, context.mkBVSort(size))
      case Lor(a, b) => context.mkBVOr(translate(slv, a, size)._1, translate(slv, b, size)._1)
      case LAnd(a, b) => context.mkBVAnd(translate(slv, a, size)._1, translate(slv, b, size)._1)
      case LXor(a, b) => context.mkBVXor(translate(slv, a, size)._1, translate(slv, b, size)._1)
      case LNot(a) => context.mkBVNot(translate(slv, a, size)._1)
      case LShift(a, b) => context.mkBVShl(translate(slv, a, size)._1, translate(slv, b, size)._1)
      case ConstantStringValue(v) => context.mkNumeral(v.hashCode.toString, context.mkIntSort)
      case Concat(expressions) => expressions.map(r => translate(slv, r.value.get, r.size)._1).reduce(context.mkConcat)
    }
    (ast, slv)
  }

  def translate(slv: Z3Solver, ast: Z3AST, constr: Constraint, size : Int): (Z3AST, Z3Solver) = {
    val ast2 = constr match {
      case AND(constrs) => context.mkAnd(constrs.map(s => translate(slv, ast, s, size)._1): _*)
      case OR(constrs) => context.mkOr(constrs.map(s => translate(slv, ast, s, size)._1): _*)
      case NOT(c) => context.mkNot(translate(slv, ast, c, size)._1)
      case E(v) => context.mkEq(ast, context.mkNumeral(v.toString(), context.mkIntSort))
      case GT(v) => context.mkBVUgt(ast, context.mkNumeral(v.toString, context.mkIntSort))
      case LTE(v) => context.mkBVUle(ast, context.mkNumeral(v.toString, context.mkIntSort))
      case GTE(v) => context.mkBVUge(ast, context.mkNumeral(v.toString, context.mkIntSort))
      case LT(v) => context.mkBVUlt(ast, context.mkNumeral(v.toString, context.mkIntSort))
      case GT_E(e) => context.mkBVUgt(ast, translate(slv, e, size)._1)
      case LT_E(e) => context.mkBVUlt(ast, translate(slv, e, size)._1)
      case LTE_E(e) => context.mkBVUle(ast, translate(slv, e, size)._1)
      case GTE_E(e) => context.mkOr(context.mkBVUgt(ast, translate(slv, e, size)._1),
        context.mkEq(ast, translate(slv, e, size)._1))
      case EQ_E(e) => context.mkEq(ast, translate(slv, e, size)._1)
    }
    (ast2, slv)
  }

  def translate(slv: Z3Solver, v: Value, size : Int): (Z3AST, Z3Solver) = {
    val (e, cts) = (v.e, v.cts)
    val (ast, slv2) = translate(slv, e, size)
    for {
      c <- cts
    } {
      slv2.assertCnstr(translate(slv, ast, c, size)._1)
    }
    (ast, slv2)
  }
}