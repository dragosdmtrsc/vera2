package org.change.v2.analysis.executor.loopdetection.translator

import z3.scala.{Z3AST, Z3Context, Z3Solver}

package object caching {

  type ExpressionID = Long
  type TranslationResult = (Z3Context, Z3Solver, Map[Long, Z3AST])

}
