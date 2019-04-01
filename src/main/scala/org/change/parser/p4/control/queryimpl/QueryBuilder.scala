package org.change.parser.p4.control.queryimpl

import org.change.parser.p4.control.SafetyQueryListener
import org.change.v2.p4.model.Switch
import z3.scala.{Z3AST, Z3Context, Z3Model, Z3Solver}

import scala.collection.mutable

class QueryBuilder(switch : Switch,
                   context : Z3Context)
  extends SafetyQueryListener[P4RootMemory] {

  val nodeToConstraint = mutable.Map.empty[Object, Z3AST]
  val errCause = mutable.Map.empty[Object, Z3AST]

  lazy val solver : Z3Solver = context.mkSolver()
  override def before(event: Object, ctx: P4RootMemory): Unit = {
    super.before(event, ctx)
    query(event, ctx).filter(!_.rootMemory.isEmpty()).foreach(mem => {
      val p = context.mkFreshBoolConst("asp")
      solver.assertCnstr(context.mkImplies(p, mem.rootMemory.condition))
      nodeToConstraint.put(event, p)
      errCause.put(event, mem.errorCause().value.asInstanceOf[ScalarValue].z3AST)
    })
  }

  def possibleLocations() : Iterator[(Object, String, Z3Model)] = {
    if (nodeToConstraint.isEmpty) {
      Iterator.empty
    } else {
      val ascertain = context.mkOr(nodeToConstraint.values.toList:_*)
      var nrPushes = 0
      solver.push()
      nrPushes = nrPushes + 1
      solver.assertCnstr(ascertain)
      if (!solver.check().get) {
        Iterator.empty
      } else {
        Iterator.continually({
          val model = solver.getModel()
          val o2 = nodeToConstraint.find(loc => {
            model.evalAs[Boolean](loc._2).getOrElse(false)
          }).get
          val casus = model.evalAs[Int](errCause(o2._1)).getOrElse(-1)
          val error = ErrorLedger.error(casus)
          solver.push()
          nrPushes = nrPushes + 1
          solver.assertCnstr(context.mkNot(o2._2))
          (o2._1, error, model)
        }).takeWhile(_ => if (solver.check().get) {
          true
        } else {
          solver.pop(nrPushes)
          false
        })
      }
    }
  }

  def query(evt : Object, in : P4RootMemory) : Option[P4RootMemory] = None

}
