package org.change.v2.p4.model.updated.instance

import org.change.v2.analysis.memory.{ProducesTagExp, Tag, TagExp}
import org.change.v2.p4.model.updated.header.HeaderDeclaration

sealed trait HeaderInstance extends ProducesTagExp {
  /**
    * Determines a tag expression for a given field (accessed by name)
    * @param fieldName
    * @return
    */
  def getTagOfField(fieldName: String): TagExp = getTagExp + layout.offsetOf(fieldName)

  def layout: HeaderDeclaration
}

class ScalarHeader(val id: String,
                   val layout: HeaderDeclaration) extends HeaderInstance {
  //TODO: This + 0 is bad and should be fixed in the type hierarchy of Intable and TagExp
  override def getTagExp(): TagExp = Tag(id) + 0
}

class ArrayHeader(val arrayName: String,
                  val index: Int,
                  val layout: HeaderDeclaration) extends HeaderInstance {
  override def getTagExp(): TagExp = Tag(arrayName) + index * layout.length
}

class MetadataInstance(override val id: String,
                       override val layout: HeaderDeclaration,
                       val values: Map[String, Int]) extends ScalarHeader(id, layout)
