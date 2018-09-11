package org.change.v2.analysis.executor.loopdetection.translator.caching

import org.change.v2.analysis.constraint._
import org.change.v2.analysis.expression.abst.Expression
import org.change.v2.analysis.expression.concrete.{ConstantValue, SymbolicValue}
import org.change.v2.analysis.expression.concrete.nonprimitive.{Minus, Plus, Reference}
import org.change.v2.analysis.memory.{MemorySpace, State, Value}
import z3.scala.{Z3AST, Z3Context}

object CachingTranslator {

  def emptyContext: TranslationResult = {
    val context = new Z3Context("MODEL" -> true)
    (context, context.mkSolver(), Map.empty)
  }

  def translateMemorySpace(context: TranslationResult)(memory: MemorySpace): TranslationResult = {
    translateValues(context)(
      (memory.symbols.values ++ memory.rawObjects.values)
      .filter(_.value.nonEmpty).map(_.value.get)
    )
  }

  def translateValues(context: TranslationResult)(values: Iterable[Value]): TranslationResult =
    values.foldLeft(context){ (currentContext, value) => {
        translateValue(currentContext)(value)._1
      }
    }

  def translateNoContext(mem: MemorySpace): TranslationResult =
    translateMemorySpace(emptyContext)(mem)

  def translateValue(context: TranslationResult)(v: Value): (TranslationResult, Z3AST) = {
    val (expression, constraints) = (v.e, v.cts)
    // translate expression
    val (contextAfterExpressionTranslation, expressionAst) = translateExpression(context)(expression)

    val finalContext = constraints.foldLeft(contextAfterExpressionTranslation) {
      (currentContext, constraint) => {
        val (newContext, constraintAST) = translateConstraint(currentContext)(expressionAst)(constraint)
        newContext._2.assertCnstr(constraintAST)
        newContext
      }
    }

    (finalContext, expressionAst)
  }

  def translateExpression(context: TranslationResult)(e: Expression): (TranslationResult, Z3AST) = {
    val astCache = context._3

    astCache.get(e.id) match {
      case None => translateNonCached(context)(e)
      case Some(ast) => (context, ast)
    }
  }

  def translateNonCached(context: TranslationResult)(e: Expression): (TranslationResult, Z3AST)  = {
    val (z3Context, z3Solver, astCache) = context

    e match {
      case Plus(a, b) =>
        val (contextWithA, astA) = translateValue(context)(a)
        val (contextWithB, astB) = translateValue(contextWithA)(b)

        val astE = z3Context.mkAdd(astA, astB)
        val newCache = contextWithB._3 + (e.id -> astE)
        ((contextWithB._1, contextWithB._2, newCache),astE)

      case Minus(a, b) =>
        val (contextWithA, astA) = translateValue(context)(a)
        val (contextWithB, astB) = translateValue(contextWithA)(b)

        val astE = z3Context.mkSub(astA, astB)
        val newCache = contextWithB._3 + (e.id -> astE)
        ((contextWithB._1, contextWithB._2, newCache),astE)
      case Reference(what, _) => translateValue(context)(what)

      case ConstantValue(v, _, _) =>
        val astE = z3Context.mkNumeral(v.toString, z3Context.mkIntSort)
        val newCache = astCache + (e.id -> astE)
        ((z3Context, z3Solver, newCache),astE)

      case SymbolicValue(_) =>
        val astE = z3Context.mkConst(e.id.toString, z3Context.mkIntSort)
        val newCache = astCache + (e.id -> astE)
        ((z3Context, z3Solver, newCache),astE)
      //      case ConstantStringValue(v) => context.mkNumeral(v.hashCode.toString, context.mkIntSort)
    }
  }

  def wrapper(subject: Z3AST)(constraint: Constraint)(context: TranslationResult): (TranslationResult, Z3AST) =
    translateConstraint(context)(subject)(constraint)

  /**
    * TODO: Document how caching works with constraints.
    * @param subject
    * @param constraint
    * @param context
    * @return
    */
  def translateConstraint(context: TranslationResult)(subject: Z3AST)(constraint: Constraint): (TranslationResult, Z3AST) =
    constraint match {
      case AND(constraints) =>
        val (constraintASTs, newContext) = sequenceContextualTranslations(context,constraints.map(wrapper(subject)))
        val constraintAST = newContext._1.mkAnd(constraintASTs: _*)
        (newContext, constraintAST)

      case OR(constraints) =>
        val (constraintASTs, newContext) = sequenceContextualTranslations(context,constraints.map(wrapper(subject)))
        val constraintAST = newContext._1.mkOr(constraintASTs: _*)
        (newContext, constraintAST)

      case NOT(c) =>
        val (newContext, innerAST) = translateConstraint(context)(subject)(c)
        val constraintAST = newContext._1.mkNot(innerAST)
        (newContext, constraintAST)

      case E(v) =>
        val constraintAst = context._1.mkEq(subject, context._1.mkNumeral(v.toString, context._1.mkIntSort))
        (context, constraintAst)

      case GT(v) =>
        val constraintAst = context._1.mkGT(subject, context._1.mkNumeral(v.toString, context._1.mkIntSort))
        (context, constraintAst)

      case LTE(v) =>
        val constraintAst = context._1.mkLE(subject, context._1.mkNumeral(v.toString, context._1.mkIntSort))
        (context, constraintAst)

      case GTE(v) =>
        val constraintAst = context._1.mkGE(subject, context._1.mkNumeral(v.toString, context._1.mkIntSort))
        (context, constraintAst)

      case LT(v) =>
        val constraintAst = context._1.mkLT(subject, context._1.mkNumeral(v.toString, context._1.mkIntSort))
        (context, constraintAst)

      case GT_E(e) =>
        val (contextWithE, astE) = translateExpression(context)(e)
        val constraintAST = contextWithE._1.mkGT(subject, astE)
        (contextWithE, constraintAST)

      case LT_E(e) =>
        val (contextWithE, astE) = translateExpression(context)(e)
        val constraintAST = contextWithE._1.mkLT(subject, astE)
        (contextWithE, constraintAST)

      case GTE_E(e) =>
        val (contextWithE, astE) = translateExpression(context)(e)
        val constraintAST = contextWithE._1.mkGE(subject, astE)
        (contextWithE, constraintAST)

      case LTE_E(e) =>
        val (contextWithE, astE) = translateExpression(context)(e)
        val constraintAST = contextWithE._1.mkLE(subject, astE)
        (contextWithE, constraintAST)

      case EQ_E(e) =>
        val (contextWithE, astE) = translateExpression(context)(e)
        val constraintAST = contextWithE._1.mkEq(subject, astE)
        (contextWithE, constraintAST)
    }

  def sequenceContextualTranslations[Context, Result](
                                                       initialContext: Context,
                                                       translations: List[Context => (Context, Result)]
                                                     ): (List[Result], Context) = {
    val (results, newContext) = translations.foldLeft((List.empty[Result], initialContext))(
      (acc, translation) => {
        val (newContext, newResult) = translation(acc._2)
        (newResult :: acc._1, newContext)
      }
    )

    (results.reverse, newContext)
  }
}
