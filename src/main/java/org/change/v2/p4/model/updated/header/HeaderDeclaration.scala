package org.change.v2.p4.model.updated.header

/**
  * A small gift from radu to symnetic.
  */
case class HeaderDeclaration (
  headerName: String,
  // Map from header offset to header name and width
  fields: Map[Int, (String, Int)],
  length: Int
) {
  // Convenience accessors
  lazy val indexOf: Map[String, Int] = fields.values.toMap
  lazy val offsetOf: Map[String, Int] = fields.map(of => of._2._1 -> of._1)
  lazy val fieldNames: Set[String] = fields.values.map(_._1).toSet
}
