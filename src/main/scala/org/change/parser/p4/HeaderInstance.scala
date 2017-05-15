package org.change.parser.p4

import org.change.v2.analysis.memory.{Intable, ProducesTagExp, Tag, TagExp}

/**
  * A small gift from radu to symnetic.
  */
sealed trait HeaderInstance extends ProducesTagExp {
  def layout: HeaderDeclaration
}

case class ScalarHeader(id: String, layout: HeaderDeclaration) extends HeaderInstance {
  //TODO: This + 0 is bad and should be fixed in the type hierarchy of Intable and TagExp
  override def getTagExp(): TagExp = Tag(id) + 0
}

case class ArrayHeader(arrayName: String, index: Int, layout: HeaderDeclaration) extends HeaderInstance {
  override def getTagExp(): TagExp = Tag(arrayName) + index * layout.length
}
