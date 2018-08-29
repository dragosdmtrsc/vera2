package org.change.v2.analysis.executor.translators

import org.change.v2.analysis.constraint._
import org.change.v2.analysis.expression.abst.Expression
import org.change.v2.analysis.expression.concrete.{ConstantBValue, ConstantStringValue, ConstantValue, SymbolicValue}
import org.change.v2.analysis.expression.concrete.nonprimitive._
import org.change.v2.analysis.memory.{MemorySpace, Value}
import z3.scala.{Z3AST, Z3Context, Z3Solver}


class Z3ListTranslator(context: Z3Context) extends Translator[List[Z3AST]] {
  def translate(mem : MemorySpace) : List[Z3AST] = {
    (mem.symbols.values ++ mem.rawObjects.values).foldLeft(Nil : List[Z3AST]) { (slv, mo) =>
      slv ++ mo.valueStack.flatMap(vs => vs.vs.flatMap(v => this.translate(v, mo.size)._2))
    }
  }

  def translate(e : Expression, size : Int) : (Z3AST, List[Z3AST]) = {
    val (ast, lst) = e match {
      case Plus(a, b) => val ta = translate(a, size)
        val tb = translate(b, size)
        (context.mkBVAdd(ta._1, tb._1), ta._2 ++ tb._2)
      case Minus(a, b) => val ta = translate(a, size)
        val tb = translate(b, size)
        (context.mkBVSub(ta._1, tb._1), ta._2 ++ tb._2)
      case Reference(what, _) => val t = translate(what, size)
        (t._1, t._2)
      case Lor(a, b) =>
        val ta = translate(a, size)
        val tb = translate(b, size)
        (context.mkBVOr(translate(a, size)._1, translate(b, size)._1), ta._2 ++ tb._2)
      case LAnd(a, b) =>
        val ta = translate(a, size)
        val tb = translate(b, size)
        (context.mkBVAnd(translate(a, size)._1, translate(b, size)._1), ta._2 ++ tb._2)
      case LXor(a, b) =>
        val ta = translate(a, size)
        val tb = translate(b, size)
        (context.mkBVXor(translate(a, size)._1, translate(b, size)._1), ta._2 ++ tb._2)
      case LNot(a) =>
        val ta = translate(a, size)
        (context.mkBVNot(translate(a, size)._1), ta._2)
      case LShift(a, b) =>
        val ta = translate(a, size)
        val tb = translate(b, size)
        (context.mkBVShl(translate(a, size)._1, translate(b, size)._1),
          ta._2 ++ tb._2)
      case ConstantStringValue(v) => (context.mkNumeral(v.hashCode.toString, context.mkIntSort), Nil)
      case ConstantBValue(v, sz) =>
        (context.mkNumeral(BigInt(v.substring(2), 16).toString, context.mkBVSort(size)), Nil)
      case Concat(expressions) =>
        val exprs = expressions.map(r => translate(r.value.get, r.size))
        (exprs.map(_._1).reduce(context.mkConcat), exprs.flatMap(_._2))
      case ConstantValue(v, _, _) => (context.mkNumeral(v.toString, context.mkBVSort(size)), Nil)
      case SymbolicValue(_) => (context.mkConst("sym" + e.id.toString, context.mkBVSort(size)), Nil)
      case ConstantStringValue(v) => (context.mkNumeral(v.hashCode.toString, context.mkIntSort), Nil)
    }
    (ast, lst)
  }

  def translate(ast : Z3AST, constr : Constraint, size : Int) : (Z3AST, List[Z3AST]) = {
    val ast2 = constr match {
      case AND(constrs) =>
        val trans = constrs.map(s => translate(ast, s, size)).unzip
        (context.mkAnd(trans._1:_*), trans._2.flatten)
      case OR(constrs) =>
        val trans = constrs.map(s => translate(ast, s, size)).unzip
        (context.mkOr(trans._1:_*), trans._2.flatten)
      case NOT(c) =>
        val trans = translate(ast, c, size)
        (context.mkNot(trans._1), trans._2)
      case E(v) => (context.mkEq(ast, context.mkNumeral(v.toString, context.mkIntSort)), Nil)
      case GT(v) => (context.mkGT(ast, context.mkNumeral(v.toString, context.mkIntSort)), Nil)
      case LTE(v) => (context.mkLE(ast, context.mkNumeral(v.toString, context.mkIntSort)), Nil)
      case GTE(v) => (context.mkGE(ast, context.mkNumeral(v.toString, context.mkIntSort)), Nil)
      case LT(v) => (context.mkLT(ast, context.mkNumeral(v.toString, context.mkIntSort)), Nil)
      case GT_E(e) =>
        val trans = translate(e, size)
        (context.mkBVUgt(ast, trans._1), trans._2)
      case LT_E(e) =>
        val trans = translate(e, size)
        (context.mkBVUlt(ast, trans._1), trans._2)
      case LTE_E(e) =>
        val trans = translate(e, size)
        (context.mkBVUle(ast, trans._1), trans._2)
      case GTE_E(e) =>
        val trans = translate(e, size)
        (context.mkOr(context.mkBVUgt(ast, trans._1), context.mkEq(ast, trans._1)), trans._2)
      case EQ_E(e) =>
        val trans = translate(e, size)
        (context.mkEq(ast, trans._1), trans._2)
    }
    ast2
  }

  def translate(v : Value, size : Int) : (Z3AST, List[Z3AST]) = {
    val (ast, vals) = translate(v.e, size)
    (ast, v.cts.flatMap(c => {
      val transd = translate(ast, c, size)
      transd._1 :: transd._2
    }) ++ vals)
  }
}


class Z3BVTranslator(context: Z3Context) extends Translator[Z3Solver] {
  private lazy val z3listTranslator = new Z3ListTranslator(context)
  override def translate(mem: MemorySpace): Z3Solver = {
    val slv = context.mkSolver()
    translate(mem, slv)
  }

  private def translateCd(condition: Condition, slv : Z3Solver) : (Z3AST, Z3Solver) = condition match {
    case OP(e, constraint, sz) => val ex = translateE(slv, e, sz)
      val c = translateC(slv, ex._1, constraint, sz)._1
      (c, slv)
    case FAND(conditions) => (context.mkAnd(conditions.map(r => {
      translateCd(r, slv)._1
    }).toList:_*), slv)
    case FOR(conditions) => (context.mkOr(conditions.map(r => {
      translateCd(r, slv)._1
    }).toList:_*), slv)
    case FNOT(r) => (context.mkNot(translateCd(r, slv)._1), slv)
    case TRUE => (context.mkTrue(), slv)
    case FALSE => (context.mkFalse(), slv)
    case _ => ???
  }

  def translate(mem : MemorySpace, slv : Z3Solver): Z3Solver = {
    //TODO: restore intersections and differences before equivalence begins again
    (mem.symbols.values ++ mem.rawObjects.values).foreach(mo =>
      mo.valueStack.foreach(vs => {
        vs.vs.foreach(v => {
          val ast = translateE(slv, v.e, mo.size)
          v.cts.foreach(c =>
            slv.assertCnstr(translateC(slv, ast._1, c, mo.size)._1)
          )
        })
      })
    )
    for (cd <- mem.pathConditions) {
      slv.assertCnstr(translateCd(cd, slv)._1)
    }
    slv
  }

  def translateE(slv: Z3Solver, e: Expression, size : Int): (Z3AST, Z3Solver) = {
    val ast = e match {
      case Plus(a, b) => context.mkBVAdd(translate(slv, a, size)._1, translate(slv, b, size)._1)
      case Minus(a, b) => context.mkBVSub(translate(slv, a, size)._1, translate(slv, b, size)._1)
      case Reference(what, _) => translate(slv, what, size)._1
      case ConstantValue(v, _, _) => context.mkNumeral(v.toString, context.mkBVSort(size))
      case ConstantBValue(v, sz) => context.mkNumeral(BigInt(v.substring(2), 16).toString, context.mkBVSort(size))
      case s : SymbolicValue => context.mkConst("sym" + s.id, context.mkBVSort(size))
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

  def translateC(slv: Z3Solver, ast: Z3AST, constr: Constraint, size : Int): (Z3AST, Z3Solver) = {
    val ast2 = constr match {
      case AND(constrs) => context.mkAnd(constrs.map(s => translateC(slv, ast, s, size)._1): _*)
      case OR(constrs) => context.mkOr(constrs.map(s => translateC(slv, ast, s, size)._1): _*)
      case NOT(c) => context.mkNot(translateC(slv, ast, c, size)._1)
      case E(v) => context.mkEq(ast, context.mkNumeral(v.toString(), context.mkIntSort))
      case GT(v) => context.mkBVUgt(ast, context.mkNumeral(v.toString, context.mkIntSort))
      case LTE(v) => context.mkBVUle(ast, context.mkNumeral(v.toString, context.mkIntSort))
      case GTE(v) => context.mkBVUge(ast, context.mkNumeral(v.toString, context.mkIntSort))
      case LT(v) => context.mkBVUlt(ast, context.mkNumeral(v.toString, context.mkIntSort))
      case GT_E(e) => context.mkBVUgt(ast, translateE(slv, e, size)._1)
      case LT_E(e) => context.mkBVUlt(ast, translateE(slv, e, size)._1)
      case LTE_E(e) => context.mkBVUle(ast, translateE(slv, e, size)._1)
      case GTE_E(e) => context.mkOr(context.mkBVUgt(ast, translateE(slv, e, size)._1),
        context.mkEq(ast, translateE(slv, e, size)._1))
      case EQ_E(e) => context.mkEq(ast, translateE(slv, e, size)._1)
    }
    (ast2, slv)
  }

  def translate(slv: Z3Solver, v: Value, size : Int): (Z3AST, Z3Solver) = {
    val (e, cts) = (v.e, v.cts)
    val (ast, slv2) = translateE(slv, e, size)
    for {
      c <- cts
    } {
      slv2.assertCnstr(translateC(slv, ast, c, size)._1)
    }
    (ast, slv2)
  }
}