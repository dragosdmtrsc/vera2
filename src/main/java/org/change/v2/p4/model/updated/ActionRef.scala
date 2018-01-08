package org.change.v2.p4.model.updated

trait ActionRef {
  def name: String
}

case class TextualActionRef(name: String) extends ActionRef