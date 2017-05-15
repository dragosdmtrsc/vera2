package org.change.parser.p4

/**
  * A small gift from radu to symnetic.
  */
sealed trait HeaderInstance

case class ScalarHeader(id: String, layout: HeaderDeclaration) extends HeaderInstance
case class ArrayHeader(arrayName: String, index: Int, layout: HeaderDeclaration) extends HeaderInstance
