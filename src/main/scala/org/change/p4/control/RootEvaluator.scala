package org.change.p4.control

import org.change.p4.control.queryimpl.{AbsValueWrapper, P4RootMemory, PlainValue, ScalarValue}
import z3.scala.{Z3AST, Z3Context, Z3Solver}

case class RootEvaluator(solver: Z3Solver) extends Evaluator {
  private val context: Z3Context = solver.context

  override def ast(expr: P4Query): Z3AST = {
    expr.asInstanceOf[AbsValueWrapper].value.asInstanceOf[ScalarValue].z3AST
  }

  override def eval(expr: P4Query): Option[P4Query] = {
    if (hasResult) {
      val valwrap = expr.as[AbsValueWrapper]
      valwrap.value match {
        case sv: ScalarValue if sv.maybeBoolean.nonEmpty =>
          Some(expr.boolVal(sv.maybeBoolean.get))
        case sv: ScalarValue if sv.maybeInt.nonEmpty =>
          Some(expr.int(sv.maybeInt.get, sv.ofType))
        case sv: ScalarValue =>
          val ast = sv.z3AST
          val model = solver.getModel()
          valwrap.value.ofType match {
            case _ =>
              Some(
                PlainValue.rv(
                  new ScalarValue(
                    z3AST = model.eval(ast).get,
                    ofType = valwrap.value.ofType
                  )
                )
              )
          }
      }
    } else {
      None
    }
  }
}

object RootEvaluator {
  def apply(mem: P4RootMemory)(implicit context: Z3Context): RootEvaluator = {
    val slv = context.mkSolver()
    slv.assertCnstr(mem.rootMemory.condition)
    RootEvaluator(slv)
  }
}
