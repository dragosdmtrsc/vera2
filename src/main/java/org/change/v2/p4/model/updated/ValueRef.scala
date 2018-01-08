package org.change.v2.p4.model.updated

trait ValueRef {
  def name: String
}

case class TextualValueRef(name: String) extends ValueRef
