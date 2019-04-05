package org.change.parser.p4.control

import org.change.v2.p4.model.Switch

trait QueryListener[T<:P4Memory] {
  def before(event : Object, ctx : T) : Unit = {}
  def after(event : Object, ctx : T) : Unit = {}
}

abstract class SafetyQueryListener[T<:P4Memory] extends QueryListener[T] {
  var stack : List[(Object, T)] = List.empty
  override def before(event: Object, ctx: T): Unit = {
    stack = (event, ctx) :: stack
  }
  override def after(event : Object, ctx : T) : Unit = {
    stack = stack.tail
  }
}

class SemaWithEvents[T<:P4Memory](s : Switch) extends
  QueryDrivenSemantics[T](s) {

  var listeners: List[QueryListener[T]] = List.empty[QueryListener[T]]

  def addListener(l : QueryListener[T]) : SemaWithEvents[T] = {
    listeners = l :: listeners
    this
  }

  override def before(obj: Object, ctx: T): Unit = {
    listeners.foreach(_.before(obj, ctx))
  }
  override def after(obj: Object, ctx: T): Unit = {
    listeners.foreach(_.after(obj, ctx))
  }
}
