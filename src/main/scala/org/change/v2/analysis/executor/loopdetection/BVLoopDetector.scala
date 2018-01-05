package org.change.v2.analysis.executor.loopdetection

import org.change.v2.analysis.executor.loopdetection.translator.caching.{CachingBVTranslator, TranslationResult}
import org.change.v2.analysis.memory.{MemorySpace, State}

object BVLoopDetector {

  def loop(state1: State,
           state2: State,
           whichMetaSymbols: Option[Iterable[String]] = None,
           whichRawSymbols: Option[Iterable[Int]] = None): Boolean = {

    val s1 = state1.memory
    val s2 = state2.memory

    // Daca simboluri diferite false

    val testedMetaSymbols = whichMetaSymbols.getOrElse(
      s1.symbols.keySet intersect s2.symbols.keySet
    )

    // TODO: Bubă aici cu inții - ar trebui tagexp !!!
    val testedRawSymbols: Iterable[Int] = whichRawSymbols.getOrElse {
      val ks1: Set[Int] = s1.rawObjects.keySet
      val ks2: Set[Int] = s2.rawObjects.keySet
      ks1 intersect ks2
    }

    val (s1MetaMappings, afterS1Context) = translateMetaValueSet(s1, testedMetaSymbols)
    val (s2MetaMappings, afterS2Context) = translateMetaValueSet(s2, testedMetaSymbols, afterS1Context)

    val (s1RawMappings, afterS1RawContext) = translateRawValueSet(s1, testedRawSymbols, afterS2Context)
    val (s2RawMappings, afterS2RawContext) = translateRawValueSet(s1, testedRawSymbols, afterS1RawContext)

    val (z3Ctx, solver, asts) = afterS2RawContext

    val pairwiseEqMeta = for {
      (symbol, s1AstId) <- s1MetaMappings
      s2AstId = s2MetaMappings(symbol)
    } yield z3Ctx.mkEq(asts(s1AstId), asts(s2AstId))

    val pairwiseEqRaw = for {
      (symbol, s1AstId) <- s1RawMappings
      s2AstId = s2RawMappings(symbol)
    } yield z3Ctx.mkEq(asts(s1AstId), asts(s2AstId))

    val allEqs = pairwiseEqMeta.toSeq ++ pairwiseEqRaw.toSeq

    if (allEqs.nonEmpty) {
      val allEq = z3Ctx.mkAnd(allEqs:_*)
      solver.assertCnstr(allEq)
      solver.check().getOrElse(false)
    } else
      true
  }

  def translateMetaValueSet(
                             s: MemorySpace,
                             which: Iterable[String],
                             context: TranslationResult = CachingBVTranslator.emptyContext):
  (Map[String, Long], TranslationResult) = {

    import scala.collection.mutable.{Map => MutableMap}
    val translationMapping: MutableMap[String, Long] = MutableMap.empty
    var crtContext = context

    for {
      symbolName <- which
      memoryObject = s.symbols(symbolName)
    } if (memoryObject.value.nonEmpty) {
      val value = memoryObject.value.get
      val expressionId = value.e.id

      crtContext = CachingBVTranslator.translateValue(crtContext)(value, memoryObject.size)._1
      translationMapping += symbolName -> expressionId
    }

    (translationMapping.toMap, crtContext)
  }

  def translateRawValueSet(
                            s: MemorySpace,
                            which: Iterable[Int],
                            context: TranslationResult = CachingBVTranslator.emptyContext):
  (Map[Int, Long], TranslationResult) = {

    import scala.collection.mutable.{Map => MutableMap}
    val translationMapping: MutableMap[Int, Long] = MutableMap.empty
    var crtContext = context

    for {
      symbolOffset <- which
      memoryObject = s.rawObjects(symbolOffset)
    } if (memoryObject.value.nonEmpty) {
      val value = memoryObject.value.get
      val expressionId = value.e.id

      crtContext = CachingBVTranslator.translateValue(crtContext)(value, memoryObject.size)._1
      translationMapping += symbolOffset -> expressionId
    }

    (translationMapping.toMap, crtContext)
  }

}