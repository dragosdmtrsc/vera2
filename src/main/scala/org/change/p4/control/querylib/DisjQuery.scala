package org.change.p4.control.querylib

import org.change.p4.control.queryimpl.{P4RootMemory, QueryBuilder}
import org.change.p4.model.Switch
import com.microsoft.z3.Context

class DisjQuery(switch: Switch,
                context: Context,
                qs: Iterable[QueryBuilder])
    extends QueryBuilder(switch, context) {
  override def query(evt: Object, in: P4RootMemory): Option[P4RootMemory] = {
    val qas = qs.map(q1 => q1.query(evt, in).filter(!_.rootMemory.isEmpty()))
    val collected = qas.collect {
      case Some(v) => v
    }
    if (collected.isEmpty) None
    else if (collected.size == 1) Some(collected.head)
    else Some(collected.head.or(collected).as[P4RootMemory])
  }
}

object DisjQuery {
  def or(switch: Switch,
         context: Context)(it: Iterable[QueryBuilder]): QueryBuilder = {
    new DisjQuery(switch, context, it)
  }
  def apply(switch: Switch,
            context: Context)(qbs: QueryBuilder*): QueryBuilder =
    or(switch, context)(qbs.toIterable)
}
