package org.change.v2.analysis.executor.translators

import org.change.v2.analysis.constraint._
import org.change.v2.analysis.expression.abst.Expression
import org.change.v2.analysis.expression.concrete.{ ConstantValue, SymbolicValue, ConstantStringValue}
import org.change.v2.analysis.expression.concrete.nonprimitive.{Minus, Plus, Reference}
import org.change.v2.analysis.memory.{MemorySpace, Value}
import z3.scala.{Z3AST, Z3Context, Z3Solver}

class Z3Translator(context: Z3Context) extends Translator[Z3Solver] {
  override def translate(mem: MemorySpace): Z3Solver = {
    val slv = context.mkSolver()
    (mem.symbols.values ++ mem.rawObjects.values).foldLeft(slv) { (slv, mo) =>
      mo.value match {
          case Some(v) => this.translate(slv, v)._2
          case _ => slv
        }
      }
  }
  def translate(slv: Z3Solver, e: Expression): (Z3AST, Z3Solver) = {
    val ast = e match {
      case Plus(a, b) => context.mkAdd(translate(slv, a)._1, translate(slv, b)._1)
      case Minus(a, b) => context.mkSub(translate(slv, a)._1, translate(slv, b)._1)
      case Reference(what, _) => translate(slv, what)._1
      case ConstantValue(v, _, _) => context.mkNumeral(v.toString, context.mkIntSort)
      case SymbolicValue(_) => context.mkConst(e.id.toString, context.mkIntSort)
      case ConstantStringValue(v) => context.mkNumeral(v.hashCode.toString, context.mkIntSort)
    }
    (ast, slv)
  }

  def translate(slv: Z3Solver, ast: Z3AST, constr: Constraint): (Z3AST, Z3Solver) = {
    val ast2 = constr match {
      case AND(constrs) => context.mkAnd(constrs.map(s => translate(slv, ast, s)._1): _*)
      case OR(constrs) => context.mkOr(constrs.map(s => translate(slv, ast, s)._1): _*)
      case NOT(c) => context.mkNot(translate(slv, ast, c)._1)
      case E(v) => context.mkEq(ast, context.mkNumeral(v.toString(), context.mkIntSort))
      case GT(v) => context.mkGT(ast, context.mkNumeral(v.toString, context.mkIntSort))
      case LTE(v) => context.mkLE(ast, context.mkNumeral(v.toString, context.mkIntSort))
      case GTE(v) => context.mkGE(ast, context.mkNumeral(v.toString, context.mkIntSort))
      case LT(v) => context.mkLT(ast, context.mkNumeral(v.toString, context.mkIntSort))
      case GT_E(e) => context.mkGT(ast, translate(slv, e)._1)
      case LT_E(e) => context.mkLT(ast, translate(slv, e)._1)
      case LTE_E(e) => context.mkLE(ast, translate(slv, e)._1)
      case GTE_E(e) => context.mkOr(context.mkGT(ast, translate(slv, e)._1),
        context.mkEq(ast, translate(slv, e)._1))
      case EQ_E(e) => context.mkEq(ast, translate(slv, e)._1)
    }
    (ast2, slv)
  }

  def translate(slv: Z3Solver, v: Value): (Z3AST, Z3Solver) = {
    val (e, cts) = (v.e, v.cts)
    val (ast, slv2) = translate(slv, e)
    for {
      c <- cts
    } {
      slv2.assertCnstr(translate(slv, ast, c)._1)
    }
    (ast, slv2)
  }
}