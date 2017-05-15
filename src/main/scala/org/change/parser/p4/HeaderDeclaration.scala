package org.change.parser.p4

/**
  * A small gift from radu to symnetic.
  */
case class HeaderDeclaration (
  headerName: String,
  // Map from header offset to header name and width
  fields: Map[Int, (String, Int)],
  length: Int
) {
  lazy val indexOf: Map[String, Int] = fields.map(_._2)
}