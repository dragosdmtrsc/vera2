package org.change.parser.p4

/**
  * A small gift from radu to symnetic.
  */
case class HeaderDeclaration (
                               headerName: String,
                               // Map from header offset to header name and width
                               fields: Map[Long, (String, Long)],
                               length: Long
                             ) {
  // Shortcut to get width by name
  lazy val indexOf: Map[String, Long] = fields.values.toMap
  lazy val offsetOf: Map[String, Long] = fields.map(of => of._2._1 -> of._1)
}