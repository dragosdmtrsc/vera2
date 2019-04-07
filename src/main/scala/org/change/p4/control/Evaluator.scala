package org.change.p4.control

import com.microsoft.z3.{AST, Expr, Model, Solver}
import org.change.p4.control.queryimpl.{AbsValueWrapper, ScalarValue}
import org.change.utils.Z3Helper._

abstract class Evaluator {
  def solver: Solver
  def constrain(axioms: List[Expr]): Evaluator = {
    for (a <- axioms)
      solver.assertCnstr(a)
    this
  }
  def constrain(axioms: Expr*): Evaluator = constrain(axioms.toList)
  def hasResult: Boolean = {
    solver.docheck().getOrElse({
      throw new IllegalStateException(solver.getReasonUnknown)
    })
  }
  def constrain(expr : P4Query) : Evaluator =
    constrain(expr.as[AbsValueWrapper].value.asInstanceOf[ScalarValue].AST)
  def ast(expr: P4Query): Expr
  def eval(expr: P4Query): Option[P4Query]
  def ever(expr: P4Query): Boolean = {
    if (!hasResult) false
    else {
      solver.push()
      solver.assertCnstr(ast(expr))
      val res = solver.docheck().get
      solver.pop()
      res
    }
  }
  def model(): Option[Model] = {
    if (hasResult) Some(solver.getModel) else None
  }

  def never(expr: P4Query): Boolean = !ever(expr)
  def always(expr: P4Query): Boolean = ever(!expr)
  def constrained(by: Expr*)(fun: => Any): Unit = constrained(by.toList)(fun)
  def constrained(by: List[Expr])(fun: => Any): Unit = {
    constrain(by)
    fun
  }
  def enumerate(limit: Int)(fun: => Option[Expr]): Unit = {
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
