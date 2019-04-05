package org.change.p4.control

import z3.scala.{Z3AST, Z3Model, Z3Solver}

abstract class Evaluator {
  def solver: Z3Solver
  def constrain(axioms: List[Z3AST]): Evaluator = {
    solver.push()
    for (a <- axioms)
      solver.assertCnstr(a)
    this
  }
  def constrain(axioms: Z3AST*): Evaluator = constrain(axioms.toList)
  def hasResult: Boolean = {
    solver.check().get
  }
  def ast(expr: P4Query): Z3AST
  def eval(expr: P4Query): Option[P4Query]
  def ever(expr: P4Query): Boolean = {
    if (!hasResult) false
    else {
      solver.push()
      solver.assertCnstr(ast(expr))
      val res = solver.check().get
      solver.pop()
      res
    }
  }
  def model(): Option[Z3Model] = {
    if (hasResult) Some(solver.getModel()) else None
  }

  def never(expr: P4Query): Boolean = !ever(expr)
  def always(expr: P4Query): Boolean = ever(!expr)
  def constrained(by: Z3AST*)(fun: => Any): Unit = constrained(by.toList)(fun)
  def constrained(by: List[Z3AST])(fun: => Any): Unit = {
    constrain(by)
    fun
    solver.pop()
  }
  def enumerate(limit: Int)(fun: => Option[Z3AST]): Unit = {
    val actualLimit = if (limit < 0) {
      Int.MaxValue
    } else {
      limit
    }
    var nscopes = 0
    var break = false
    for (i <- 0 until actualLimit
         if !break) {
      val append = fun
      if (append.isEmpty) {
        break = true
      } else {
        nscopes = nscopes + 1
        solver.push()
        solver.assertCnstr(append.get)
      }
    }
    solver.pop(nscopes)
  }
}
