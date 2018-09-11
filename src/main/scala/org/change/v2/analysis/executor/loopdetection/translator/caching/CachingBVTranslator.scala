package org.change.v2.analysis.executor.loopdetection.translator.caching

import org.change.v2.analysis.constraint._
import org.change.v2.analysis.expression.abst.Expression
import org.change.v2.analysis.expression.concrete.{ConstantStringValue, ConstantValue, SymbolicValue}
import org.change.v2.analysis.expression.concrete.nonprimitive._
import org.change.v2.analysis.memory.{MemoryObject, MemorySpace, Value}
import z3.scala.{Z3AST, Z3Context, Z3Solver}

object CachingBVTranslator {

  def emptyContext: TranslationResult = {
    val context = new Z3Context("MODEL" -> true)
    (context, context.mkSolver(), Map.empty)
  }

  def translateNoContext(mem: MemorySpace): TranslationResult =
    translateMemorySpace(emptyContext)(mem)

  def translateMemorySpace(context: TranslationResult)(memory: MemorySpace): TranslationResult = {
    translateMemoryObjects(context)(
      (memory.symbols.values ++ memory.rawObjects.values)
        .filter(_.value.nonEmpty)
    )
  }

  def translateMemoryObjects(context: TranslationResult)(memoryObjects: Iterable[MemoryObject]): TranslationResult =
    memoryObjects.foldLeft(context){ (currentContext, memoryObject) => {
      translateValue(currentContext)(memoryObject.value.get, memoryObject.size)._1
    }
  }

  def translateValue(context: TranslationResult)(v: Value, size: Int): (TranslationResult, Z3AST) = {
    val (expression, constraints) = (v.e, v.cts)
    // translate expression
    val (contextAfterExpressionTranslation, expressionAst) = translateExpression(context)(expression, size)

    val finalContext = constraints.foldLeft(contextAfterExpressionTranslation) {
      (currentContext, constraint) => {
        val (newContext, constraintAST) = translateConstraint(currentContext)(expressionAst)(constraint, size)
        newContext._2.assertCnstr(constraintAST)
        newContext
      }
    }

    (finalContext, expressionAst)
  }

  def translateExpression(context: TranslationResult)(e: Expression, size: Int): (TranslationResult, Z3AST) = {
    val astCache = context._3

    astCache.get(e.id) match {
      case None => translateNonCached(context)(e, size)
      case Some(ast) => (context, ast)
    }
  }

  def translateNonCached(context: TranslationResult)(e: Expression, size: Int): (TranslationResult, Z3AST) = {
    val (z3Context, z3Solver, astCache) = context

    e match {
      case Plus(a, b) =>
        val (contextWithA, astA) = translateValue(context)(a, size)
        val (contextWithB, astB) = translateValue(contextWithA)(b, size)

        val astE = z3Context.mkBVAdd(astA, astB)
        val newCache = contextWithB._3 + (e.id -> astE)
        ((contextWithB._1, contextWithB._2, newCache), astE)

      case Minus(a, b) =>
        val (contextWithA, astA) = translateValue(context)(a, size)
        val (contextWithB, astB) = translateValue(contextWithA)(b, size)

        val astE = z3Context.mkBVSub(astA, astB)
        val newCache = contextWithB._3 + (e.id -> astE)
        ((contextWithB._1, contextWithB._2, newCache), astE)
      case Reference(what, _) => translateValue(context)(what, size)

      case ConstantValue(v, _, _) =>
        val astE = z3Context.mkNumeral(v.toString, z3Context.mkBVSort(size))
        val newCache = astCache + (e.id -> astE)
        ((z3Context, z3Solver, newCache), astE)
      case ConstantStringValue(v) =>
        val astE = z3Context.mkNumeral(v.hashCode.toString, z3Context.mkBVSort(size))
        val newCache = astCache + (e.id -> astE)
        ((z3Context, z3Solver, newCache), astE)
      case SymbolicValue(_) =>
        val astE = z3Context.mkConst(e.id.toString, z3Context.mkBVSort(size))
        val newCache = astCache + (e.id -> astE)
        ((z3Context, z3Solver, newCache), astE)
      //      case ConstantStringValue(v) => context.mkNumeral(v.hashCode.toString, context.mkIntSort)

      /**
        * BV - specific expressions
        */
      case Lor(a, b) =>
        val (contextWithA, astA) = translateValue(context)(a, size)
        val (contextWithB, astB) = translateValue(contextWithA)(b, size)

        val astE = z3Context.mkBVOr(astA, astB)
        val newCache = contextWithB._3 + (e.id -> astE)
        ((contextWithB._1, contextWithB._2, newCache), astE)

      case LAnd(a, b) =>
        val (contextWithA, astA) = translateValue(context)(a, size)
        val (contextWithB, astB) = translateValue(contextWithA)(b, size)

        val astE = z3Context.mkBVAnd(astA, astB)
        val newCache = contextWithB._3 + (e.id -> astE)
        ((contextWithB._1, contextWithB._2, newCache), astE)

      case LXor(a, b) =>
        val (contextWithA, astA) = translateValue(context)(a, size)
        val (contextWithB, astB) = translateValue(contextWithA)(b, size)

        val astE = z3Context.mkBVXor(astA, astB)
        val newCache = contextWithB._3 + (e.id -> astE)
        ((contextWithB._1, contextWithB._2, newCache), astE)

      case LNot(a) =>
        val (contextWithA, astA) = translateValue(context)(a, size)

        val astE = z3Context.mkBVNot(astA)
        val newCache = contextWithA._3 + (e.id -> astE)
        ((contextWithA._1, contextWithA._2, newCache), astE)

      /**
        * Concat translation
        */
      case Concat(memoryObjects) =>
        val translationResult = memoryObjects.scanLeft(context, null: Z3AST){
          (crtContext: (TranslationResult, Z3AST), memoryObject: MemoryObject) => {
            val value = memoryObject.value.get
            translateValue(crtContext._1)(value, memoryObject.size)
          }
        }

        val newContext = translationResult.last._1
        val astE = translationResult.map(_._2).filter(_ != null).reduce(newContext._1.mkConcat)
        val newCache = newContext._3 + (e.id -> astE)
        ((newContext._1, newContext._2, newCache), astE)
    }
  }

  def wrapper(expression: Expression, size: Int)(context: TranslationResult): (TranslationResult, Z3AST) =
    translateExpression(context)(expression, size)

  def wrapper(subject: Z3AST, size: Int)(constraint: Constraint)(context: TranslationResult): (TranslationResult, Z3AST) =
    translateConstraint(context)(subject)(constraint, size)

  def translateConstraint(context: TranslationResult)
                         (subject: Z3AST)
                         (constraint: Constraint, size: Int): (TranslationResult, Z3AST) = constraint match {
    case AND(constraints) =>
      val (constraintASTs, newContext) = sequenceContextualTranslations(context,constraints.map(wrapper(subject, size)))
      val constraintAST = newContext._1.mkAnd(constraintASTs: _*)
      (newContext, constraintAST)

    case OR(constraints) =>
      val (constraintASTs, newContext) = sequenceContextualTranslations(context,constraints.map(wrapper(subject, size)))
      val constraintAST = newContext._1.mkOr(constraintASTs: _*)
      (newContext, constraintAST)

    case NOT(c) =>
      val (newContext, innerAST) = translateConstraint(context)(subject)(c, size)
      val constraintAST = newContext._1.mkNot(innerAST)
      (newContext, constraintAST)

    case E(v) =>
      val constraintAst = context._1.mkEq(subject, context._1.mkNumeral(v.toString, context._1.mkIntSort))
      (context, constraintAst)

    case GT(v) =>
      val constraintAst = context._1.mkBVUgt(subject, context._1.mkNumeral(v.toString, context._1.mkIntSort))
      (context, constraintAst)

    case LTE(v) =>
      val constraintAst = context._1.mkBVUle(subject, context._1.mkNumeral(v.toString, context._1.mkIntSort))
      (context, constraintAst)

    case GTE(v) =>
      val constraintAst = context._1.mkBVUge(subject, context._1.mkNumeral(v.toString, context._1.mkIntSort))
      (context, constraintAst)

    case LT(v) =>
      val constraintAst = context._1.mkBVUlt(subject, context._1.mkNumeral(v.toString, context._1.mkIntSort))
      (context, constraintAst)

    case GT_E(e) =>
      val (contextWithE, astE) = translateExpression(context)(e, size)
      val constraintAST = contextWithE._1.mkBVUgt(subject, astE)
      (contextWithE, constraintAST)

    case LT_E(e) =>
      val (contextWithE, astE) = translateExpression(context)(e, size)
      val constraintAST = contextWithE._1.mkBVUlt(subject, astE)
      (contextWithE, constraintAST)

    case GTE_E(e) =>
      val (contextWithE, astE) = translateExpression(context)(e, size)
      val constraintAST = contextWithE._1.mkBVUge(subject, astE)
      (contextWithE, constraintAST)

    case LTE_E(e) =>
      val (contextWithE, astE) = translateExpression(context)(e, size)
      val constraintAST = contextWithE._1.mkBVUle(subject, astE)
      (contextWithE, constraintAST)

    case EQ_E(e) =>
      val (contextWithE, astE) = translateExpression(context)(e, size)
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
