package org.change.parser.p4

import org.change.v2.analysis.memory.{ProducesTagExp, Tag, TagExp}

sealed trait P4Instance extends ProducesTagExp {
  def layout: HeaderDeclaration
}

sealed trait HeaderInstance extends P4Instance {
  /**
    * Determines a tag expression for a given field (accessed by name)
    * @param fieldName
    * @return
    */
  def getTagOfField(fieldName: String): TagExp = getTagExp + layout.offsetOf(fieldName)
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
