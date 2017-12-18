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
  // Shortcut to get width by name
  lazy val indexOf: Map[String, Int] = fields.values.toMap
  lazy val offsetOf: Map[String, Int] = fields.map(of => of._2._1 -> of._1)
}