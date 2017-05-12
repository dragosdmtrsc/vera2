package org.change.parser.p4

import scala.collection.mutable.{Map => MutableMap}

/**
  * A small gift from radu to symnetic.
  */
case class HeaderDeclaration(
                              headerName: String,
                              // Map from header offset to header name and width
                              fields: MutableMap[Int, (String, Int)] = MutableMap()
                            )


object HeaderDeclarationParser {

  // Map from header name to header declaration
  private val declaredHeaders: MutableMap[String, HeaderDeclaration] = MutableMap()

}
