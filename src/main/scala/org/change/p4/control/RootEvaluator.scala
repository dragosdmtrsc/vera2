package org.change.p4.control

import org.change.p4.control.queryimpl.{AbsValueWrapper, P4RootMemory, PlainValue, ScalarValue}
import com.microsoft.z3.{AST, Context, Expr, Solver}
import org.change.utils.Z3Helper
import org.change.utils.Z3Helper._

case class RootEvaluator(solver: Solver, context: Context) extends Evaluator {
  override def ast(expr: P4Query): Expr = {
    expr.asInstanceOf[AbsValueWrapper].value.asInstanceOf[ScalarValue].AST
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
          val ast = sv.AST
          val model = solver.getModel()
          valwrap.value.ofType match {
            case _ =>
              Some(
                PlainValue.rv(
                  new ScalarValue(
                    AST = model.eval(ast, false),
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
  def apply(mem: P4RootMemory)(implicit context: Context): RootEvaluator = {
    val slv = context.mkSolver()
    slv.assertCnstr(mem.rootMemory.condition)
    RootEvaluator(slv, context)
  }
}
