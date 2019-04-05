package org.change.p4.control.querylib

import org.change.p4.control.queryimpl.{P4RootMemory, QueryBuilder}
import org.change.p4.model.Switch
import z3.scala.Z3Context

class DisjQuery(switch: Switch,
                context: Z3Context,
                q1: QueryBuilder,
                q2: QueryBuilder)
    extends QueryBuilder(switch, context) {
  override def query(evt: Object, in: P4RootMemory): Option[P4RootMemory] = {
    val qa = q1.query(evt, in).filter(!_.rootMemory.isEmpty())
    val qb = q2.query(evt, in).filter(!_.rootMemory.isEmpty())
    (qa, qb) match {
      case (Some(lr), Some(rr)) =>
        Some((lr || rr).as[P4RootMemory])
      case (Some(lr), None) =>
        Some(lr)
      case (None, Some(rr)) =>
        Some(rr)
      case (None, None) =>
        None
    }
  }
}

object DisjQuery {
  def or(switch: Switch,
         context: Z3Context)(it: Iterable[QueryBuilder]): QueryBuilder = {
    if (it.isEmpty)
      throw new IllegalArgumentException(
        "disj query must have at least one query"
      )
    else if (it.size == 1)
      it.head
    else {
      it.tail.foldLeft(it.head)((acc, x) => {
        new DisjQuery(switch, context, acc, x)
      })
    }
  }
  def apply(switch: Switch,
            context: Z3Context)(qbs: QueryBuilder*): QueryBuilder =
    or(switch, context)(qbs.toIterable)
}
