package org.change.p4.control.queryimpl

import org.change.p4.control.SafetyQueryListener
import org.change.p4.model.Switch
import z3.scala.{Z3AST, Z3Context, Z3Solver}

import scala.collection.mutable

class QueryBuilder(switch: Switch, context: Z3Context)
    extends SafetyQueryListener[P4RootMemory] {
  val nodeToConstraint = mutable.Map.empty[Object, Z3AST]
  val errCause = mutable.Map.empty[Object, Z3AST]

  val locSelectors = mutable.Map.empty[Object, Z3AST]

  def getLocationSelector(obj : Object) : Z3AST = {
    locSelectors.getOrElseUpdate(obj, context.mkFreshBoolConst("loc"))
  }

  private lazy val solver: Z3Solver = context.mkSolver()
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
          .put(event, mem.errorCause().value.asInstanceOf[ScalarValue].z3AST)
      })
  }

  def buildSolver(): Z3Solver = {
    if (nodeToConstraint.nonEmpty) {
      val ascertain = context.simplifyAst(context.mkOr(nodeToConstraint.values.toList: _*))
      solver.assertCnstr(ascertain)
    }
    solver
  }
  def query(evt: Object, in: P4RootMemory): Option[P4RootMemory] = None
}
