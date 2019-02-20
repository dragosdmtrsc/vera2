package org.change.v3

import z3.scala.{Z3AST, Z3ASTVector, Z3Context}

package object semantics {
  type Condition = Z3AST
  type SimplePathCondition = List[Z3AST]

  val context: Z3Context = new Z3Context()
}
