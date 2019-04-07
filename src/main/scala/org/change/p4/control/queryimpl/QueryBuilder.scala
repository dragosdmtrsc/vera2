package org.change.p4.control.queryimpl

import com.microsoft.z3.{BoolExpr, Context, Expr, Solver}
import org.change.p4.control.SafetyQueryListener
import org.change.p4.model.Switch
import org.change.utils.Z3Helper._

import scala.collection.mutable

class QueryBuilder(switch: Switch, context: Context)
    extends SafetyQueryListener[P4RootMemory] {
  val nodeToConstraint = mutable.Map.empty[Object, BoolExpr]
  val errCause = mutable.Map.empty[Object, Expr]

  val locSelectors = mutable.Map.empty[Object, BoolExpr]

  def getLocationSelector(obj : Object) : BoolExpr = {
    locSelectors.getOrElseUpdate(obj, context.mkFreshBoolConst("loc"))
  }

  private lazy val solver: Solver = {
    val ackermanized = context.mkTactic("ackermannize_bv")
    val simplify = context.mkTactic("simplify")
    val qfbv = context.mkTactic("qfbv")
    val eq2bv = context.mkTactic("eq2bv")
    val dt2bv = context.mkTactic("dt2bv")
    val macros = context.mkTactic("macro-finder")
    val t1 = context.andThen(
      simplify,
      macros,
      dt2bv,
      eq2bv,
      ackermanized,
      simplify,
      qfbv)
    val slv = context.mkSolver(t1)
    val parms = context.mkParams()
    parms.add("mbqi", false)
    slv.setParameters(parms)
    slv
  }
  override def before(event: Object, ctx: P4RootMemory): Unit = {
    super.before(event, ctx)
    query(event, ctx)
      .filter(!_.rootMemory.isEmpty())
      .foreach(mem => {
        nodeToConstraint.put(event,
          context.mkAnd(mem.rootMemory.condition,
            getLocationSelector(event)
          ))
        errCause
          .put(event,
            mem.errorCause().value.asInstanceOf[ScalarValue].AST)
      })
  }

  def buildSolver(): Solver = {
    if (nodeToConstraint.nonEmpty) {
      val ascertain = context.mkOr(nodeToConstraint.values.toSeq:_*).simplify().asInstanceOf[BoolExpr]
      solver.add(ascertain)
    }
    solver
  }
  def query(evt: Object, in: P4RootMemory): Option[P4RootMemory] = None
}
